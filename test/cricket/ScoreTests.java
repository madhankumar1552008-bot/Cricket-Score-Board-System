package cricket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Dependency-free tests. Run with:
 *   java -cp out cricket.ScoreTests
 * Exits with a non-zero code if any check fails.
 */
public class ScoreTests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        oversConversion();
        ballByBallScoring();
        inningsLimits();
        resultCalculation();
        simulatorIsRepeatable();
        simulatorRespectsRules();
        predictorCountsEverything();
        storeRoundTrip();

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void oversConversion() {
        check("19.3 overs = 117 balls", CricketScore.oversToBalls(19.3) == 117);
        check("20.0 overs = 120 balls", CricketScore.oversToBalls(20) == 120);
        check("0.0 overs = 0 balls", CricketScore.oversToBalls(0) == 0);
        check("19.7 is rejected", throwsIllegalArgument(() -> CricketScore.oversToBalls(19.7)));
        check("19.25 is rejected", throwsIllegalArgument(() -> CricketScore.oversToBalls(19.25)));
        check("20.1 is rejected", throwsIllegalArgument(() -> CricketScore.oversToBalls(20.1)));
    }

    private static void ballByBallScoring() {
        CricketScore s = new CricketScore("Test XI");
        s.addRuns(4);
        s.addRuns(0);
        s.addWide();
        s.addNoBall(2);
        s.addWicket(0);
        check("runs add up (4 + 1 + 3)", s.getRuns() == 8);
        check("wicket counted", s.getWickets() == 1);
        check("wide and no-ball are not legal balls", s.getBalls() == 3);
        check("extras counted", s.getExtras() == 2);
        check("overs string is 0.3", s.getOversString().equals("0.3"));
    }

    private static void inningsLimits() {
        CricketScore allOut = new CricketScore("A");
        for (int i = 0; i < 10; i++) {
            allOut.addWicket(0);
        }
        check("all out after 10 wickets", allOut.isAllOut() && allOut.isInningsOver());
        check("cannot score once all out", throwsIllegalState(() -> allOut.addRuns(1)));

        CricketScore full = new CricketScore("B");
        for (int i = 0; i < 120; i++) {
            full.addRuns(1);
        }
        check("innings ends after 120 balls", full.isInningsOver());
        check("20.0 overs displayed", full.getOversString().equals("20.0"));
        check("run rate is 6.00", Math.abs(full.getRunRate() - 6.0) < 1e-9);

        CricketScore chaser = new CricketScore("C");
        chaser.addRuns(6);
        check("chase over once target reached", chaser.isInningsOver(6));
        check("chase not over below target", !chaser.isInningsOver(7));
    }

    private static void resultCalculation() {
        CricketScore india = new IndiaScore(200, 10, 20.0);
        CricketScore aus = new AustraliaScore(160, 10, 20.0);
        MatchResult r = MatchResult.decide(india, aus);
        check("first innings team wins by 40 runs",
                "India".equals(r.getWinner()) && r.getDescription().equals("India won by 40 runs"));

        CricketScore chase = new AustraliaScore(201, 4, 18.2);
        MatchResult r2 = MatchResult.decide(india, chase);
        check("chasing team wins by wickets",
                r2.getDescription().equals("Australia won by 6 wickets (10 balls left)"));

        MatchResult r3 = MatchResult.decide(new CricketScore("X", 150, 8, 20.0),
                new CricketScore("Y", 150, 9, 20.0));
        check("equal runs is a tie", r3.isTie() && r3.getWinner() == null);

        check("all-out chaser cannot win", throwsIllegalArgument(() ->
                MatchResult.decide(new CricketScore("P", 100, 5, 20.0),
                        new CricketScore("Q", 120, 10, 20.0))));
    }

    private static void simulatorIsRepeatable() {
        MatchResult a = new MatchSimulator(42L, false).simulateMatch("India", "Australia", 1.0, 1.0);
        MatchResult b = new MatchSimulator(42L, false).simulateMatch("India", "Australia", 1.0, 1.0);
        check("same seed gives the same match",
                a.getFirst().scoreLine().equals(b.getFirst().scoreLine())
                        && a.getSecond().scoreLine().equals(b.getSecond().scoreLine())
                        && a.getDescription().equals(b.getDescription()));
    }

    private static void simulatorRespectsRules() {
        boolean ok = true;
        for (long seed = 0; seed < 300; seed++) {
            MatchResult r = new MatchSimulator(seed, false).simulateMatch("A", "B", 1.0, 1.0);
            for (CricketScore s : new CricketScore[] {r.getFirst(), r.getSecond()}) {
                if (s.getBalls() > 120 || s.getWickets() > 10 || s.getRuns() < 0) {
                    ok = false;
                }
            }
        }
        check("300 simulated matches obey T20 limits", ok);

        int strongWins = 0;
        for (long seed = 0; seed < 400; seed++) {
            MatchResult r = new MatchSimulator(seed, false).simulateMatch("Strong", "Weak", 1.4, 0.7);
            if ("Strong".equals(r.getWinner())) {
                strongWins++;
            }
        }
        check("stronger team wins most matches (" + strongWins + "/400)", strongWins > 240);
    }

    private static void predictorCountsEverything() throws Exception {
        WinPredictor.Prediction p = WinPredictor.run("A", 1.0, "B", 1.0, 400, 7L, 4);
        check("every simulated match is counted", p.winsA + p.winsB + p.ties == 400);
        check("parallel run is repeatable", p.winsA == WinPredictor.run("A", 1.0, "B", 1.0, 400, 7L, 2).winsA);
    }

    private static void storeRoundTrip() throws IOException {
        Path temp = Files.createTempFile("history", ".csv");
        Files.delete(temp);
        try {
            MatchStore store = new MatchStore(temp.toString());
            check("empty history for missing file", store.load().isEmpty());
            store.save(MatchResult.decide(new IndiaScore(200, 10, 20.0), new AustraliaScore(160, 10, 20.0)), "test");
            store.save(MatchResult.decide(new IndiaScore(100, 10, 20.0), new AustraliaScore(101, 3, 15.2)), "test");
            List<String[]> rows = store.load();
            check("two matches saved", rows.size() == 2);
            check("saved fields readable",
                    rows.get(0)[2].equals("India") && rows.get(0)[6].equals("India won by 40 runs"));
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    // ---------- tiny test helpers ----------

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }

    private static boolean throwsIllegalArgument(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean throwsIllegalState(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
