package revision_scheduler.service;

import revision_scheduler.model.Module;
import revision_scheduler.model.StudySession;
import revision_scheduler.model.Topic;
import revision_scheduler.repository.ModuleRepository;
import revision_scheduler.repository.StudySessionRepository;
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
    private StudySessionRepository studySessionRepository;

    /**
     * Generates a revision schedule from today until the latest exam date,
     * using a priority-based greedy allocation algorithm.
     *
     * Priority score = (difficulty * remainingHours) / daysUntilExam
     * Higher score = more urgent. Each day, available hours are allocated
     * to the highest-priority topics first.
     */
    public List<StudySession> generateSchedule(double dailyAvailableHours) {
        List<Module> modules = moduleRepository.findAll();
        LocalDate today = LocalDate.now();

        // Clear ALL uncompleted sessions (including missed ones) so their hours get rescheduled
        List<StudySession> existing = studySessionRepository.findAll();
        for (StudySession s : existing) {
            if (!s.isCompleted()) {
                studySessionRepository.delete(s);
            }
        }

        // Gather all topics with remaining work, across all modules
        List<Topic> allTopics = new ArrayList<>();
        for (Module m : modules) {
            if (m.getExamDate().isBefore(today)) continue; // skip past exams
            for (Topic t : m.getTopics()) {
                if (t.getRemainingHours() > 0) {
                    allTopics.add(t);
                }
            }
        }

        if (allTopics.isEmpty()) {
            return new ArrayList<>();
        }

        // Find the furthest exam date to know our scheduling horizon
        LocalDate latestExam = modules.stream()
                .map(Module::getExamDate)
                .filter(d -> !d.isBefore(today))
                .max(LocalDate::compareTo)
                .orElse(today.plusDays(7));

        List<StudySession> generatedSessions = new ArrayList<>();

        // Track remaining hours needed per topic (working copy)
        Map<Long, Double> remainingHours = new HashMap<>();
        for (Topic t : allTopics) {
            remainingHours.put(t.getId(), t.getRemainingHours());
        }

        LocalDate cursor = today;
        while (cursor.isBefore(latestExam.plusDays(1)) && stillHasWork(remainingHours)) {
            double hoursLeftToday = dailyAvailableHours;

            // Recalculate priority scores fresh each day, since urgency changes as exams approach
            List<Topic> sortedByPriority = new ArrayList<>(allTopics);
            LocalDate finalCursor = cursor;
            sortedByPriority.sort((a, b) -> {
                double scoreA = priorityScore(a, remainingHours.get(a.getId()), finalCursor);
                double scoreB = priorityScore(b, remainingHours.get(b.getId()), finalCursor);
                return Double.compare(scoreB, scoreA); // descending
            });

            for (Topic topic : sortedByPriority) {
                if (hoursLeftToday <= 0) break;

                double remaining = remainingHours.get(topic.getId());
                if (remaining <= 0) continue;

                // Don't schedule a topic after its module's exam date
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
        if (daysUntilExam <= 0) daysUntilExam = 1; // avoid divide-by-zero, treat as maximally urgent
        return (topic.getDifficulty() * remainingHours) / daysUntilExam;
    }

    private boolean stillHasWork(Map<Long, Double> remainingHours) {
        return remainingHours.values().stream().anyMatch(h -> h > 0.01);
    }
}