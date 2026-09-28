package revision_scheduler.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    private LocalDate date;
    private double allocatedHours;
    private boolean completed = false;

    public StudySession() {}

    public StudySession(Topic topic, LocalDate date, double allocatedHours) {
        this.topic = topic;
        this.date = date;
        this.allocatedHours = allocatedHours;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public double getAllocatedHours() { return allocatedHours; }
    public void setAllocatedHours(double allocatedHours) { this.allocatedHours = allocatedHours; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
