package revision_scheduler.controller;

import revision_scheduler.model.StudySession;
import revision_scheduler.model.Topic;
import revision_scheduler.repository.StudySessionRepository;
import revision_scheduler.repository.TopicRepository;
import revision_scheduler.service.SchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "*")
public class SchedulingController {

    @Autowired
    private SchedulingService schedulingService;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @GetMapping
    public List<StudySession> getSchedule() {
        return studySessionRepository.findAll();
    }

    @PostMapping("/generate")
    public List<StudySession> generateSchedule(@RequestBody GenerateRequest request) {
        return schedulingService.generateSchedule(request.dailyAvailableHours);
    }

    @PutMapping("/{id}/complete")
    public StudySession markComplete(@PathVariable Long id) {
        StudySession session = studySessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.isCompleted()) {
            return session; // already counted, don't double-add hours
        }

        Topic topic = session.getTopic();
        topic.setHoursCompleted(topic.getHoursCompleted() + session.getAllocatedHours());
        topicRepository.save(topic);

        session.setCompleted(true);
        return studySessionRepository.save(session);
    }

    public static class GenerateRequest {
        public double dailyAvailableHours;
    }
}