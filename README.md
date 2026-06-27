# Speedrun Timer

A CLI-based speedrun timer in Java. It measures time across multiple segments
(splits) of a run, compares against best times, and persists runs and personal
bests as JSON.

The core is a pure, GUI-free logic library — the CLI is only a thin
presentation layer. A later GUI (Android, SWT, …) can attach to the same logic
core through the same observer interface.

## Features

- **Run control** via a state machine: `start`, `split`, `pause`, `resume`,
  `reset`.
- **Multiple splits per run** with absolute timestamps (no accumulating drift
  over long runs).
- **Pauses** are correctly subtracted from the elapsed time.
- **Comparison modes** against a reference, swappable at runtime:
    - vs. Personal Best
    - vs. Sum of Best (golden splits)
    - vs. Average
- **JSON persistence** (Gson): save runs, load personal bests.
- **Observable events** (`onStart`, `onSplit`, `onPause`, `onResume`,
  `onFinish`, `onReset`) for decoupled presentation layers.
## Build & Run

Requires JDK 21 and Maven.

```bash
mvn clean install                 # build + run all quality gates
mvn clean package                 # produce the self-contained executable JAR
java -jar target/speedrunner.jar  # uses ./speedruns.json (pass a path to override)
```

No data file is needed to start. On launch you either pick an existing run from
the list or **configure a new one** — entering the game, the category and the
split names — which is then saved to the data file. A run **auto-finishes** once
it reaches the configured number of splits, and finishing **improves that
route's personal best** in the file. A sample data file lives in
[`samples/`](samples/) if you want to start from one.

## Commands (CLI)

| Command  | Effect                                              |
|----------|-----------------------------------------------------|
| `start`  | starts the run (only from the idle state)           |
| `split`  | records a split (only while running)                |
| `pause`  | pauses the running run                              |
| `resume` | resumes a paused run                                |
| `reset`  | resets the timer                                     |
| `status` | shows current time, splits, comparison vs. PB       |
| `quit`   | exits the application (also `exit`)                 |

Illegal transitions (e.g. `split` while idle) are rejected, not silently
ignored.

## Architecture

Layered, with dependencies pointing inward only. Four design patterns, each
motivated by the domain:

- **State** — `TimerState` (abstract) plus `IdleState`, `RunningState`,
  `PausedState`, `FinishedState`. Each state holds only a back-reference to its
  orchestrator and is created fresh on every transition (no singletons); the
  context (`SpeedrunTimer`) holds the run data and passes the timestamp through.
  `entry()`/`exit()` encapsulate transition side effects.
- **Strategy** — `CompareStrategy` for the comparison modes (PB / Sum of Best /
  Average), selectable per speedrun category and applied by `SplitCalculator`.
  The `status` command loads the `PersonalBest` and runs the active strategy
  through the core (`SpeedrunTimer.compareToPersonalBest()`) to show the delta
  vs. PB; the CLI only renders the result.
- **Repository** — `SplitRepository` port with a JSON adapter; injected via the
  constructor (manual dependency injection from `Main`, no singleton).
- **Observer** — `TimerListener` as the event interface; the CLI implements it,
  and a later GUI can too.
  The time source sits behind a `Clock` port so the time-dependent logic is
  deterministically testable (a test clock instead of the real clock).

The class diagram lives under `docs/architecture/`.

## Tests & Quality

- JUnit 5, focused on the state transitions (including illegal transitions) and
  the comparison logic.
- Time-dependent tests use a fixed test clock — never real time in a test.
- Quality gates in the build: Google Java Format, CheckStyle, PMD, SpotBugs,
  JaCoCo (coverage threshold).
- Logging via Log4J2, never `System.out` / `printStackTrace`.
## Notes

- Time measurement in this version is based on `Instant`. For drift-free
  duration measurement against clock jumps (NTP, DST), a monotonic source
  (`System.nanoTime()`) would be the technically correct choice — deliberately
  noted as a later extension.


