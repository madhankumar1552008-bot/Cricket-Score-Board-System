# Cricket Scoreboard (T20)

A console toolkit for scoring, simulating and predicting T20 cricket matches, written in plain Java with no external libraries.

It started as a Java lab exercise on inheritance, interfaces, exceptions and multithreading, and grew into a small working application. The original lab concepts are all still here and each one now does a real job.

## Features

| Menu option | What it does |
|---|---|
| **1. Enter final scores** | Type in two final scores (runs, wickets, overs like `19.3`), get the scoreboard and the result ("won by 40 runs" / "won by 6 wickets, 10 balls left" / tie). |
| **2. Live ball-by-ball scoring** | Score a full match yourself: `0-6` runs, `w` wicket, `wd` wide, `nb4` no-ball hit for four. Shows run rate, over summaries and the required runs while chasing. |
| **3. Simulate a match** | Plays a full T20 match with a toss and live commentary (wickets, fours, sixes, end-of-over summaries). Team strength is adjustable and a seed makes a match repeatable. |
| **4. Win predictor** | Plays thousands of matches **in parallel on a thread pool** and reports win percentages and average scores. |
| **5. Match history** | Every finished match is saved to `match_history.csv`; this lists them. |
| **6. Concurrency lab** | The original `wait()` / `notify()` / `notifyAll()` demo and a deliberate deadlock demo. |

## Quick start

You need JDK 11 or newer (`java -version` to check).

**Linux / macOS**

```bash
./run.sh          # compile and start the app
./run.sh test     # compile and run the tests
```

**Windows (PowerShell, for example the VS Code terminal)**

```powershell
javac -d out (Get-ChildItem -Recurse src,test -Filter *.java | ForEach-Object FullName)
java -cp out app.Main          # start the app
java -cp out cricket.ScoreTests   # run the tests
```

**Any system, manual**

```bash
mkdir -p out
javac -d out $(find src test -name "*.java")
java -cp out app.Main
```

## Example: simulated match (seed 7)

```
Toss: India won and chose to bat first.

--- 1st innings: India ---
  1.0  FOUR!  -> 8/0
  1.3  SIX!  -> 14/0
  ...
Match Type: T20
===== CRICKET SCOREBOARD =====
Team Name: India (Home Team)
Runs: 133
Wickets: 8
Overs: 20.0
Run Rate: 6.65
Extras: 6
----------------------
Team Name: Australia
Runs: 134
Wickets: 7
Overs: 16.4
Run Rate: 8.04
Extras: 4
----------------------
RESULT: Australia won by 3 wickets (20 balls left)
```

## Example: win predictor

```
Simulating 5000 matches on 2 threads...

===== WIN PREDICTION =====
India        |###################.....................|  48.4%
Australia    |####################....................|  50.8%
Tie          |........................................|   0.8%
Finished in 141 ms
```

With equal strengths the two sides come out close to 50/50, as they should. Raise one team's strength and its win rate climbs.

## Project structure

```
cricket-scoreboard/
├── src/
│   ├── cricket/                  # the model and logic
│   │   ├── Match.java            # abstract base class (team name)
│   │   ├── ScoreDisplay.java     # interface
│   │   ├── CricketScore.java     # one innings: runs, wickets, balls, extras
│   │   ├── IndiaScore.java       # subclass, overrides displayTeam()
│   │   ├── AustraliaScore.java   # subclass
│   │   ├── ScoreFactory.java     # picks the right subclass for a team name
│   │   ├── MatchResult.java      # who won, and by how much
│   │   ├── MatchSimulator.java   # ball-by-ball match engine
│   │   ├── WinPredictor.java     # parallel simulations (ExecutorService)
│   │   └── MatchStore.java       # CSV history (java.nio.file)
│   └── app/                      # the console application
│       ├── Main.java             # menu
│       ├── ConsoleInput.java     # safe, re-prompting input
│       └── ConcurrencyLab.java   # wait/notify and deadlock demos
├── test/cricket/ScoreTests.java  # dependency-free tests
├── .github/workflows/ci.yml      # compiles and tests on every push
└── run.sh
```

## Java concepts and where they live

| Concept | Where |
|---|---|
| Abstract class | `Match` |
| Interface | `ScoreDisplay`, implemented by `CricketScore` |
| Inheritance and method overriding | `IndiaScore.displayTeam()`, `CricketScore.showMatchDetails()` |
| Method overloading | `CricketScore.displayScore()` and `displayScore(String title)` |
| Static members | `CricketScore.matchType`, `displayMatchType()` |
| Factory pattern | `ScoreFactory` |
| Exceptions and validation | `IllegalArgumentException` in score constructors, `IllegalStateException` when scoring after an innings ends, re-prompting in `ConsoleInput` |
| `wait` / `notify` / `notifyAll` | `ConcurrencyLab.ThreadSignal` |
| Deadlock and daemon threads | `ConcurrencyLab.DeadlockThread` |
| Thread pool, `Callable`, `Future` | `WinPredictor` |
| File I/O | `MatchStore` |

## Design notes

- **Overs are stored as balls.** `19.3` overs is 117 balls internally, so there are no floating-point errors when scoring ball by ball. Input like `19.7` or `19.25` is rejected.
- **Wides and no-balls** add runs but do not use up a ball, as in real cricket.
- **The simulator is repeatable.** The same seed always gives the same match. The win predictor gives every match its own simulator, so threads share no state.
- **Why the two teams' average scores differ in the predictor:** a chasing team stops batting the moment it reaches the target, so it records fewer runs on average than a team batting first.
- **The deadlock demo uses daemon threads**, so the two stuck threads do not stop the program from exiting.

## Ideas for extending it

- Per-batter and per-bowler scorecards
- Super overs for tied matches
- Reading fixtures from a file
- A JavaFX or web front end on top of the `cricket` package (it has no console code in the logic classes other than the display methods)

## License

MIT, see [LICENSE](LICENSE).

Author: [Nagarajan M](https://github.com/Nagarajan-Marisamy)
