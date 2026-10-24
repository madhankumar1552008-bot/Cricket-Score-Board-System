package app;

import cricket.CricketScore;
import cricket.MatchResult;
import cricket.MatchSimulator;
import cricket.MatchStore;
import cricket.ScoreFactory;
import cricket.WinPredictor;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

/**
 * Cricket Scoreboard: a console T20 scoring toolkit.
 *
 * Run it and pick an option from the menu.
 */
public class Main {

    private static final MatchStore STORE = new MatchStore("match_history.csv");

    public static void main(String[] args) {
        ConsoleInput in = new ConsoleInput(System.in);
        System.out.println("=====================================");
        System.out.println("        CRICKET SCOREBOARD (T20)     ");
        System.out.println("=====================================");

        try {
            boolean running = true;
            while (running) {
                printMenu();
                int choice = in.integer("Choose an option", 0, 6);
                System.out.println();
                switch (choice) {
                    case 1:
                        quickMatch(in);
                        break;
                    case 2:
                        liveScoring(in);
                        break;
                    case 3:
                        simulateMatch(in);
                        break;
                    case 4:
                        predictWinner(in);
                        break;
                    case 5:
                        showHistory();
                        break;
                    case 6:
                        concurrencyLab(in);
                        break;
                    default:
                        running = false;
                        break;
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed.");
        } finally {
            System.out.println("Scoreboard program ended.");
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1. Enter final scores (India vs Australia)");
        System.out.println("2. Live ball-by-ball scoring");
        System.out.println("3. Simulate a match");
        System.out.println("4. Win predictor (parallel simulations)");
        System.out.println("5. Match history");
        System.out.println("6. Concurrency lab (wait/notify and deadlock demos)");
        System.out.println("0. Exit");
    }

    // ------------------------------------------------------------------
    // 1. Quick match: type in final scores
    // ------------------------------------------------------------------

    private static void quickMatch(ConsoleInput in) {
        int firstTeam = in.integer("Who batted first? 1 = India, 2 = Australia", 1, 2);
        String firstName = firstTeam == 1 ? "India" : "Australia";
        String secondName = firstTeam == 1 ? "Australia" : "India";

        CricketScore first = readScore(in, firstName);
        CricketScore second = readScore(in, secondName);

        try {
            MatchResult result = MatchResult.decide(first, second);
            result.print();
            save(result, "manual");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static CricketScore readScore(ConsoleInput in, String team) {
        while (true) {
            System.out.println("\nEnter " + team + " Score");
            int runs = in.integer("Runs", 0, 999);
            int wickets = in.integer("Wickets", 0, CricketScore.MAX_WICKETS);
            double overs = in.decimal("Overs (e.g. 19.3 means 19 overs and 3 balls)", 0, CricketScore.MAX_OVERS);
            try {
                return ScoreFactory.create(team, runs, wickets, overs);
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage() + " Please enter the score again.");
            }
        }
    }

    // ------------------------------------------------------------------
    // 2. Live scoring
    // ------------------------------------------------------------------

    private static void liveScoring(ConsoleInput in) {
        String teamA = in.nameOrDefault("Team batting first", "India");
        String teamB = in.nameOrDefault("Team batting second", "Australia");
        if (teamA.equalsIgnoreCase(teamB)) {
            System.out.println("The two teams need different names.");
            return;
        }

        System.out.println();
        System.out.println("Commands for each ball:");
        System.out.println("  0-6      runs off the bat");
        System.out.println("  w        wicket   (w1 = wicket with 1 run completed)");
        System.out.println("  wd       wide");
        System.out.println("  nb       no-ball  (nb4 = no-ball hit for 4)");
        System.out.println("  q        end this innings early");

        CricketScore first = ScoreFactory.create(teamA);
        System.out.println("\n--- 1st innings: " + teamA + " ---");
        playLiveInnings(in, first, 0);

        int target = first.getRuns() + 1;
        CricketScore second = ScoreFactory.create(teamB);
        System.out.println("\n--- 2nd innings: " + teamB + " need " + target + " to win ---");
        playLiveInnings(in, second, target);

        try {
            MatchResult result = MatchResult.decide(first, second);
            result.print();
            save(result, "live");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void playLiveInnings(ConsoleInput in, CricketScore score, int target) {
        int runsAtOverStart = score.getRuns();

        while (!score.isInningsOver(target)) {
            String status = score.getTeamName() + " " + score.getRuns() + "/" + score.getWickets()
                    + " (" + score.getOversString() + ")";
            if (target > 0) {
                int needed = target - score.getRuns();
                int ballsLeft = CricketScore.MAX_BALLS - score.getBalls();
                status += "  need " + needed + " from " + ballsLeft;
            }
            String command = in.line(status + " > ").toLowerCase();

            int ballsBefore = score.getBalls();
            try {
                if (command.equals("q")) {
                    System.out.println("Innings ended early.");
                    return;
                } else if (command.equals("wd")) {
                    score.addWide();
                } else if (command.startsWith("nb")) {
                    int offBat = command.length() > 2 ? Integer.parseInt(command.substring(2)) : 0;
                    score.addNoBall(offBat);
                } else if (command.startsWith("w")) {
                    int completed = command.length() > 1 ? Integer.parseInt(command.substring(1)) : 0;
                    score.addWicket(completed);
                    System.out.println("  WICKET! " + score.getWickets() + " down.");
                } else {
                    score.addRuns(Integer.parseInt(command));
                }
            } catch (IllegalArgumentException e) { // also covers NumberFormatException
                System.out.println("  Not a valid command. Use 0-6, w, wd, nb or q.");
                continue;
            }

            boolean overCompleted = score.getBalls() != ballsBefore && score.getBalls() % 6 == 0;
            if (overCompleted) {
                System.out.printf("  End of over %d: %d/%d (%d runs in the over, run rate %.2f)%n",
                        score.getBalls() / 6, score.getRuns(), score.getWickets(),
                        score.getRuns() - runsAtOverStart, score.getRunRate());
                runsAtOverStart = score.getRuns();
            }
        }
        System.out.println("Innings complete: " + score.scoreLine());
    }

    // ------------------------------------------------------------------
    // 3. Simulate
    // ------------------------------------------------------------------

    private static void simulateMatch(ConsoleInput in) {
        String teamA = in.nameOrDefault("First team", "India");
        String teamB = in.nameOrDefault("Second team", "Australia");
        if (teamA.equalsIgnoreCase(teamB)) {
            System.out.println("The two teams need different names.");
            return;
        }
        double ratingA = in.decimalOrDefault(teamA + " strength (0.5 weak - 1.5 strong)", 0.5, 1.5, 1.0);
        double ratingB = in.decimalOrDefault(teamB + " strength (0.5 weak - 1.5 strong)", 0.5, 1.5, 1.0);
        long seed = in.longOrDefault("Seed for a repeatable match (Enter for random): ", System.nanoTime());

        System.out.println();
        MatchSimulator simulator = new MatchSimulator(seed, true);
        MatchResult result = simulator.simulateMatch(teamA, teamB, ratingA, ratingB);
        result.print();
        save(result, "simulated");
    }

    // ------------------------------------------------------------------
    // 4. Win predictor (uses a thread pool)
    // ------------------------------------------------------------------

    private static void predictWinner(ConsoleInput in) {
        String teamA = in.nameOrDefault("First team", "India");
        String teamB = in.nameOrDefault("Second team", "Australia");
        if (teamA.equalsIgnoreCase(teamB)) {
            System.out.println("The two teams need different names.");
            return;
        }
        double ratingA = in.decimalOrDefault(teamA + " strength (0.5 weak - 1.5 strong)", 0.5, 1.5, 1.0);
        double ratingB = in.decimalOrDefault(teamB + " strength (0.5 weak - 1.5 strong)", 0.5, 1.5, 1.0);
        int simulations = in.integerOrDefault("Number of simulated matches", 10, 100000, 5000);

        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("\nSimulating " + simulations + " matches on " + threads + " threads...");

        try {
            long started = System.nanoTime();
            WinPredictor.Prediction p = WinPredictor.run(
                    teamA, ratingA, teamB, ratingB, simulations, System.nanoTime(), threads);
            long millis = (System.nanoTime() - started) / 1_000_000;

            System.out.println();
            System.out.println("===== WIN PREDICTION =====");
            printBar(teamA, p.percentA());
            printBar(teamB, p.percentB());
            if (p.ties > 0) {
                printBar("Tie", p.percentTies());
            }
            System.out.printf("Average score: %s %.1f, %s %.1f%n", teamA, p.averageA, teamB, p.averageB);
            System.out.println("Finished in " + millis + " ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Prediction was interrupted.");
        } catch (ExecutionException e) {
            System.out.println("Prediction failed: " + e.getCause());
        }
    }

    private static void printBar(String label, double percent) {
        int filled = (int) Math.round(percent / 2.5);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            bar.append(i < filled ? '#' : '.');
        }
        System.out.printf("%-12s |%s| %5.1f%%%n", label, bar, percent);
    }

    // ------------------------------------------------------------------
    // 5. History
    // ------------------------------------------------------------------

    private static void showHistory() {
        try {
            List<String[]> rows = STORE.load();
            if (rows.isEmpty()) {
                System.out.println("No matches saved yet. Play or simulate one first.");
                return;
            }
            System.out.println("===== MATCH HISTORY (" + rows.size() + " matches) =====");
            System.out.printf("%-17s %-9s %-10s %-15s %-10s %-15s %s%n",
                    "Date", "Mode", "Batted 1st", "Score", "Batted 2nd", "Score", "Result");
            for (String[] r : rows) {
                System.out.printf("%-17s %-9s %-10s %-15s %-10s %-15s %s%n",
                        r[0], r[1], r[2], r[3], r[4], r[5], r[6]);
            }
        } catch (IOException e) {
            System.out.println("Could not read history: " + e.getMessage());
        }
    }

    private static void save(MatchResult result, String mode) {
        try {
            STORE.save(result, mode);
            System.out.println("(Saved to " + STORE.getFile() + ")");
        } catch (IOException e) {
            System.out.println("Could not save match: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 6. Concurrency lab
    // ------------------------------------------------------------------

    private static void concurrencyLab(ConsoleInput in) {
        System.out.println("1. wait / notify / notifyAll demo");
        System.out.println("2. Deadlock demo");
        int choice = in.integer("Choose a demo", 1, 2);
        try {
            if (choice == 1) {
                ConcurrencyLab.runWaitNotifyDemo();
            } else {
                ConcurrencyLab.runDeadlockDemo();
            }
        } catch (InterruptedException e) {
            System.out.println("Main thread was interrupted.");
            Thread.currentThread().interrupt();
        }
    }
}
