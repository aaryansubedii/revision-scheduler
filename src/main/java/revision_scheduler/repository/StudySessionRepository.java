package revision_scheduler.repository;

import revision_scheduler.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByDate(java.time.LocalDate date);
}