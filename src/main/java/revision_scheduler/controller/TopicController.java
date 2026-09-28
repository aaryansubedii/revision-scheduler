package revision_scheduler.controller;

import revision_scheduler.model.Module;
import revision_scheduler.model.Topic;
import revision_scheduler.repository.ModuleRepository;
import revision_scheduler.repository.TopicRepository;
import revision_scheduler.service.GroqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "*")
public class TopicController {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private GroqService groqService;

    @GetMapping
    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    @PostMapping
    public Topic createTopic(@RequestBody TopicRequest request) {
        Module module = moduleRepository.findById(request.moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found"));

        Topic topic = new Topic(request.name, request.difficulty, request.estimatedHoursNeeded, module);
        return topicRepository.save(topic);
    }

    // AI: breaks a module into topics with difficulty + hours, and saves them
    @PostMapping("/generate")
    public List<Topic> generateTopics(@RequestBody GenerateTopicsRequest request) {
        Module module = moduleRepository.findById(request.moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found"));

        List<Topic> saved = new ArrayList<>();
        for (GroqService.TopicSuggestion s : groqService.suggestTopics(module.getName())) {
            saved.add(topicRepository.save(new Topic(s.name(), s.difficulty(), s.estimatedHours(), module)));
        }
        return saved;
    }

    @PutMapping("/{id}/progress")
    public Topic updateProgress(@PathVariable Long id, @RequestBody ProgressUpdate update) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found"));
        topic.setHoursCompleted(update.hoursCompleted);
        return topicRepository.save(topic);
    }

    @DeleteMapping("/{id}")
    public void deleteTopic(@PathVariable Long id) {
        topicRepository.deleteById(id);
    }

    public static class TopicRequest {
        public String name;
        public int difficulty;
        public double estimatedHoursNeeded;
        public Long moduleId;
    }

    public static class GenerateTopicsRequest {
        public Long moduleId;
    }

    public static class ProgressUpdate {
        public double hoursCompleted;
    }
}