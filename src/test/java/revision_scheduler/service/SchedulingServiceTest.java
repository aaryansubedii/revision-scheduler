package revision_scheduler.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import revision_scheduler.model.Module;
import revision_scheduler.model.StudySession;
import revision_scheduler.model.Topic;
import revision_scheduler.repository.ModuleRepository;
import revision_scheduler.repository.StudySessionRepository;
import revision_scheduler.repository.TopicRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SchedulingServiceTest {

    @Autowired
    private SchedulingService schedulingService;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @BeforeEach
    void cleanDatabase() {
        // Start each test with a clean slate so tests don't affect each other
        studySessionRepository.deleteAll();
        topicRepository.deleteAll();
        moduleRepository.deleteAll();
    }

    @Test
    void generateSchedule_allocatesExactlyTheHoursNeeded() {
        // A single topic needing 6 hours, exam in 3 days, 2 hours/day available.
        // We expect the algorithm to schedule exactly 6 hours total, no more, no less.
        Module module = moduleRepository.save(new Module("Test Module", LocalDate.now().plusDays(3)));
        topicRepository.save(new Topic("Topic A", 3, 6.0, module));

        List<StudySession> schedule = schedulingService.generateSchedule(2.0);

        double totalHours = schedule.stream().mapToDouble(StudySession::getAllocatedHours).sum();
        assertEquals(6.0, totalHours, 0.01, "Total scheduled hours should exactly match the topic's requirement");
    }

    @Test
    void generateSchedule_prioritizesHigherDifficultyTopicFirst() {
        // Two topics, same exam date. The harder one (difficulty 5) should
        // get scheduled on the earliest day, before the easier one (difficulty 1).
        Module module = moduleRepository.save(new Module("Test Module", LocalDate.now().plusDays(5)));
        Topic easy = topicRepository.save(new Topic("Easy Topic", 1, 4.0, module));
        Topic hard = topicRepository.save(new Topic("Hard Topic", 5, 4.0, module));

        List<StudySession> schedule = schedulingService.generateSchedule(2.0);

        StudySession firstSession = schedule.stream()
                .min((a, b) -> a.getDate().compareTo(b.getDate()))
                .orElseThrow();

        assertEquals(hard.getId(), firstSession.getTopic().getId(),
                "The higher-difficulty topic should be scheduled first when exam dates are equal");
    }

    @Test
    void generateSchedule_neverSchedulesPastTheExamDate() {
        // No session should ever be dated on or after the topic's own exam date.
        Module module = moduleRepository.save(new Module("Test Module", LocalDate.now().plusDays(2)));
        topicRepository.save(new Topic("Topic A", 3, 10.0, module)); // more hours than time allows

        List<StudySession> schedule = schedulingService.generateSchedule(2.0);

        boolean anyPastExam = schedule.stream()
                .anyMatch(s -> !s.getDate().isBefore(module.getExamDate()));

        assertFalse(anyPastExam, "No session should be scheduled on or after the exam date");
    }

    @Test
    void generateSchedule_recalculatesAfterSessionCompleted() {
        // After completing a 2-hour session, regenerating the schedule should
        // allocate 2 fewer total hours for that topic, proving progress is
        // actually taken into account rather than restarting from scratch.
        Module module = moduleRepository.save(new Module("Test Module", LocalDate.now().plusDays(5)));
        Topic topic = topicRepository.save(new Topic("Topic A", 3, 6.0, module));

        List<StudySession> firstSchedule = schedulingService.generateSchedule(2.0);
        StudySession firstSession = firstSchedule.get(0);

        // Simulate completing that session, same as SchedulingController does
        topic.setHoursCompleted(topic.getHoursCompleted() + firstSession.getAllocatedHours());
        topicRepository.save(topic);
        firstSession.setCompleted(true);
        studySessionRepository.save(firstSession);

        List<StudySession> secondSchedule = schedulingService.generateSchedule(2.0);
        double remainingScheduledHours = secondSchedule.stream()
                .mapToDouble(StudySession::getAllocatedHours)
                .sum();

        assertEquals(6.0 - firstSession.getAllocatedHours(), remainingScheduledHours, 0.01,
                "Remaining scheduled hours should reflect the completed session's hours being subtracted");
    }

    @Test
    void generateSchedule_returnsEmptyWhenNoTopicsExist() {
        // No modules or topics at all -> the algorithm should return an empty
        // list, not throw an exception.
        List<StudySession> schedule = schedulingService.generateSchedule(2.0);
        assertTrue(schedule.isEmpty(), "Schedule should be empty when there are no topics to plan");
    }
}