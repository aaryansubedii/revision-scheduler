package revision_scheduler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int difficulty; // 1-5
    private double estimatedHoursNeeded;
    private double hoursCompleted = 0.0;

    @ManyToOne
    @JoinColumn(name = "module_id")
    @JsonIgnore
    private Module module;

    public Topic() {}

    public Topic(String name, int difficulty, double estimatedHoursNeeded, Module module) {
        this.name = name;
        this.difficulty = difficulty;
        this.estimatedHoursNeeded = estimatedHoursNeeded;
        this.module = module;
    }

    public double getRemainingHours() {
        return Math.max(0, estimatedHoursNeeded - hoursCompleted);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public double getEstimatedHoursNeeded() { return estimatedHoursNeeded; }
    public void setEstimatedHoursNeeded(double estimatedHoursNeeded) { this.estimatedHoursNeeded = estimatedHoursNeeded; }

    public double getHoursCompleted() { return hoursCompleted; }
    public void setHoursCompleted(double hoursCompleted) { this.hoursCompleted = hoursCompleted; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }
}
