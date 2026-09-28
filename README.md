# 🧠 Adaptive Revision Scheduler

A full-stack app that builds a revision timetable from your modules and exam dates, then re-plans it as you make progress.

## How it works
1. Add a module with its exam date
2. An LLM (Groq) breaks the module into topics, each with a difficulty (1-5) and estimated revision hours
3. A greedy scheduling algorithm allocates your daily study hours across all topics
4. Mark sessions as done. Regenerating the schedule subtracts completed hours and re-plans the rest

## The algorithm
Each day, every topic gets a priority score:

    priority = (difficulty × remaining_hours) / days_until_exam

Topics are sorted by score and that day's hours are allocated to the highest-priority topics first. Scores are recalculated daily, because urgency rises as an exam approaches. Complexity is roughly O(D · T log T) for D days and T topics.

## Tech stack
**Backend:** Java 21, Spring Boot, Spring Data JPA, H2 (file-based)
**AI:** Groq API (topic breakdown)
**Frontend:** HTML, CSS, vanilla JavaScript

## Architecture
Entity → Repository → Service → Controller. The scheduling logic lives in `SchedulingService`, and the LLM integration in `GroqService`.

## Run locally
1. Clone the repo
2. Create `secrets.properties` in the project root: `groq.api.key=YOUR_KEY`
3. Run `RevisionSchedulerApplication` (backend on http://localhost:8080)
4. Open `frontend/index.html` in a browser

## API
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET/POST | /api/modules | List / create modules |
| POST | /api/topics/generate | AI-generate topics for a module |
| POST | /api/schedule/generate | Generate or recalculate the schedule |
| GET | /api/schedule | View all sessions |
| PUT | /api/schedule/{id}/complete | Mark a session complete |

## Known limitations
- If daily hours can't cover all topics before an exam, the schedule under-allocates without warning
- No user accounts (single-user)
- Greedy heuristic, not a provably optimal schedule

## Future improvements
- Warn when a schedule is infeasible
- Spaced-repetition revisits
- Deployment with a public demo