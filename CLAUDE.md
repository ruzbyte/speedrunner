# Project Development Guide

This is a **CLI speedrun timer in Java 21**, built as a university practical
with strict quality gates. You act as a co-pilot: you may generate boilerplate,
tests, commit messages, and plugin configuration. But **every design decision is
made by the human** and must be explainable by them in the oral assessment. Do
not introduce an architecture or pattern that deviates from what is documented
here — if something is unclear, ask instead of guessing.

## Quick Reference

| Item | Value |
|------|-------|
| **Project** | speedrunner |
| **Language** | Java 21 |
| **Architecture** | layered |
| **Persistence** | JSON (Gson), no database |
| **Main Branch** | main |
| **Build / Test** | `mvn clean install` / `mvn test` |
| **Lint / Quality** | `mvn checkstyle:check pmd:check spotbugs:check` (or `mvn verify`) |

## Architecture (binding)

Layered, dependencies point inward only. Target diagram lives in
`docs/architecture/speedrun_timer_architecture.puml`. Source root `src/main/java`,
test root `src/test/java`. Four design patterns, each motivated by the domain:

- **State** — abstract `TimerState`, concrete `IdleState`, `RunningState`,
  `PausedState`, `FinishedState`.
    - Each state holds a **back-reference to its orchestrator** `SpeedrunTimer`
      (passed via the constructor) and is **created fresh on each transition**
      (`timer.setState(new RunningState(timer), now)`). **No singletons.**
    - States hold **no run data** — the splits, start instant and accumulated
      pause all live on the orchestrator `SpeedrunTimer`; a state's only field is
      the orchestrator reference.
    - Every state command method takes `(Instant now)` — the timestamp is
      **passed through, not re-read inside the states** (states never call
      `clock.now()`).
    - The **state** triggers transitions (`timer.setState(new XState(timer),
      now)`), not the orchestrator.
    - `entry()`/`exit()` encapsulate transition side effects (e.g.
      `FinishedState.entry` builds the immutable `Run` and saves it via the
      repository).
    - States reach the repository and listeners **only through package-private
      helper methods on the orchestrator** (`timer.save(...)`,
      `timer.fireFinish(...)`, etc.); the orchestrator owns all collaborators
      (clock, repository, calculator).
    - Illegal transitions throw `IllegalStateException` — never silently ignored.
- **Strategy** — `CompareStrategy` for comparison modes (`VsPersonalBest`,
  `VsSumOfBest`, `VsAverage`), used by `SplitCalculator`.
- **Repository** — `SplitRepository` interface, `JsonSplitRepository` (Gson).
  **Manual dependency injection**: the instance is created once in `Main` and
  passed through the constructor. **No singleton pattern.**
- **Observer** — `TimerListener` (`onStart`/`onSplit`/`onPause`/`onResume`/
  `onFinish`/`onReset`). Events fire at the point of origin, including from
  `entry()`/`exit()`. The CLI implements the interface; a later GUI does too.
## Time Measurement

- Based on `java.time.Instant`, behind a `Clock` port (`now()`).
- **Store absolute timestamps, do not sum deltas** — this prevents *accumulation*
  drift (the rounding error of summing many per-split deltas). It does **not**
  address *wall-clock-jump* drift from NTP/DST corrections; a monotonic source
  (`System.nanoTime()`) would be needed for that and is deliberately deferred
  (see README "Notes").
- Pauses are subtracted from elapsed time as accumulated pause duration, tracked
  as a `Duration` on the context (`SpeedrunTimer`), not inside the records.
- Tests use a fixed test clock, never the real clock.
## Domain Objects

`Split`, `Run`, `PersonalBest` as **records** (immutable). Validation in the
compact constructor (e.g. no empty category name).

The in-progress run is **accumulated in the context** (`SpeedrunTimer` holds a
mutable split list, the start `Instant`, and the accumulated pause `Duration`);
the immutable `Run` record is constructed **once** in `FinishedState.entry` and
handed to the repository. Records are never mutated.

## Code Standards

- Follow Java community best practices.
- **Logging only via Log4J2.** Never `System.out.println()` or
  `e.printStackTrace()`.
- Code must conform to **Google Java Style** (format gate in the build).
- **CheckStyle, PMD, SpotBugs** must be clean — zero warnings/errors.
  `mvn clean install` must fail on violations.
- **JaCoCo**: at least 70% line coverage.
- All new code must have tests, with **real assertions**, no empty lifecycle
  methods. Structure tests as **Arrange-Act-Assert**.
- No tests or properties in `src/main/java`.
- Run quality gates before every commit.
- Git commits: `type(scope): subject` (conventional format).
- **NO AI/tool references in commit messages.**
- Repo hygiene: never commit generated files, IDE configs, or logs.
## Key Rules

<!-- These survive context compaction and are always visible -->

1. Follow the layered architecture and the four patterns defined above.
2. Persistence is JSON via the `SplitRepository` port — no database, no raw SQL.
3. Run lint/quality gates before every commit.
4. Never generate code the human cannot explain line by line. When in doubt:
   small step, explanation, then continue.
5. Do not add patterns or abstractions beyond the four above "because it looks
   good" — they are deliberately chosen and justified.
## Development Workflow

1. Check current branch and status.
2. Implement changes following the architecture pattern.
3. Run tests: `mvn test`.
4. Run quality gates: `mvn checkstyle:check pmd:check spotbugs:check`.
5. Commit with conventional format. The human reviews and signs the commit —
   "AI writes the diff, the human signs the commit."
## Git Workflow

- Meaningful, iterative commits.
- Feature branches (e.g. `feature/state-machine`, `feature/persistence`).
## Agents

See `.claude/AGENTS_README.md` for the agent team documentation.

## Imported Rules

@import .claude/rules/java-conventions.md
@import .claude/rules/testing.md
