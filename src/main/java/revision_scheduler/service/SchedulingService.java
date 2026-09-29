package revision_scheduler.service;

import revision_scheduler.model.Module;
import revision_scheduler.model.StudySession;
import revision_scheduler.model.Topic;
import revision_scheduler.repository.ModuleRepository;
import revision_scheduler.repository.StudySessionRepository;
import revision_scheduler.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SchedulingService {

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    public List<StudySession> generateSchedule(double dailyAvailableHours) {
        List<Module> modules = moduleRepository.findAll();
        LocalDate today = LocalDate.now();

        List<StudySession> existing = studySessionRepository.findAll();
        for (StudySession s : existing) {
            if (!s.isCompleted()) {
                studySessionRepository.delete(s);
            }
        }

        // Fetch topics explicitly per module instead of relying on lazy-loaded
        // module.getTopics(), which requires an open Hibernate session that
        // may not exist by the time this method runs (e.g. in tests, or across
        // request boundaries).
        List<Topic> allTopics = new ArrayList<>();
        for (Module m : modules) {
            if (m.getExamDate().isBefore(today)) continue;
            List<Topic> topicsForModule = topicRepository.findByModuleId(m.getId());
            for (Topic t : topicsForModule) {
                if (t.getRemainingHours() > 0) {
                    allTopics.add(t);
                }
            }
        }

        if (allTopics.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDate latestExam = modules.stream()
                .map(Module::getExamDate)
                .filter(d -> !d.isBefore(today))
                .max(LocalDate::compareTo)
                .orElse(today.plusDays(7));

        List<StudySession> generatedSessions = new ArrayList<>();

        Map<Long, Double> remainingHours = new HashMap<>();
        for (Topic t : allTopics) {
            remainingHours.put(t.getId(), t.getRemainingHours());
        }

        LocalDate cursor = today;
        while (cursor.isBefore(latestExam.plusDays(1)) && stillHasWork(remainingHours)) {
            double hoursLeftToday = dailyAvailableHours;

            List<Topic> sortedByPriority = new ArrayList<>(allTopics);
            LocalDate finalCursor = cursor;
            sortedByPriority.sort((a, b) -> {
                double scoreA = priorityScore(a, remainingHours.get(a.getId()), finalCursor);
                double scoreB = priorityScore(b, remainingHours.get(b.getId()), finalCursor);
                return Double.compare(scoreB, scoreA);
            });

            for (Topic topic : sortedByPriority) {
                if (hoursLeftToday <= 0) break;

                double remaining = remainingHours.get(topic.getId());
                if (remaining <= 0) continue;

                if (!cursor.isBefore(topic.getModule().getExamDate())) continue;

                double allocate = Math.min(remaining, hoursLeftToday);
                if (allocate <= 0) continue;

                StudySession session = new StudySession(topic, cursor, allocate);
                generatedSessions.add(session);

                remainingHours.put(topic.getId(), remaining - allocate);
                hoursLeftToday -= allocate;
            }

            cursor = cursor.plusDays(1);
        }

        return studySessionRepository.saveAll(generatedSessions);
    }

    private double priorityScore(Topic topic, double remainingHours, LocalDate fromDate) {
        long daysUntilExam = ChronoUnit.DAYS.between(fromDate, topic.getModule().getExamDate());
        if (daysUntilExam <= 0) daysUntilExam = 1;
        return (topic.getDifficulty() * remainingHours) / daysUntilExam;
    }

    private boolean stillHasWork(Map<Long, Double> remainingHours) {
        return remainingHours.values().stream().anyMatch(h -> h > 0.01);
    }
}