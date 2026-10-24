package cricket;

/**
 * One team's innings in a T20 match.
 *
 * Overs are stored internally as a count of legal balls, so 19.3 overs is
 * 117 balls. That avoids floating point mistakes when scoring ball by ball.
 */
public class CricketScore extends Match implements ScoreDisplay {

    public static final int MAX_OVERS = 20;
    public static final int MAX_BALLS = MAX_OVERS * 6;
    public static final int MAX_WICKETS = 10;

    public static String matchType = "T20";

    private int runs;
    private int wickets;
    private int balls;
    private int extras;

    /** Start a fresh innings (0/0 after 0.0 overs). */
    public CricketScore(String teamName) {
        super(teamName);
    }

    /** Create an innings from a final score, for example 200 runs, 10 wickets, 20.0 overs. */
    public CricketScore(String teamName, int runs, int wickets, double overs) {
        super(teamName);
        if (runs < 0) {
            throw new IllegalArgumentException("Runs cannot be negative");
        }
        if (wickets < 0 || wickets > MAX_WICKETS) {
            throw new IllegalArgumentException("Wickets must be between 0 and " + MAX_WICKETS);
        }
        this.runs = runs;
        this.wickets = wickets;
        this.balls = oversToBalls(overs);
    }

    /** Converts cricket notation (19.3 = 19 overs, 3 balls) into a ball count. */
    public static int oversToBalls(double overs) {
        if (overs < 0 || overs > MAX_OVERS) {
            throw new IllegalArgumentException("Overs must be between 0 and " + MAX_OVERS);
        }
        int whole = (int) overs;
        double fraction = (overs - whole) * 10;
        int part = (int) Math.round(fraction);
        if (part > 5 || Math.abs(fraction - part) > 0.01) {
            throw new IllegalArgumentException(
                    "Overs must look like 19.3 (only 0 to 5 balls after the dot)");
        }
        return whole * 6 + part;
    }

    // ---------- ball by ball scoring ----------

    /** A legal delivery that scored 0 to 7 runs off the bat. */
    public void addRuns(int r) {
        checkRuns(r);
        ensureInPlay();
        runs += r;
        balls++;
    }

    /** A wicket falls on a legal delivery; runsCompleted covers things like a run out. */
    public void addWicket(int runsCompleted) {
        checkRuns(runsCompleted);
        ensureInPlay();
        runs += runsCompleted;
        wickets++;
        balls++;
    }

    /** A wide: one extra run, and the ball is not counted. */
    public void addWide() {
        ensureInPlay();
        runs++;
        extras++;
    }

    /** A no-ball: one extra run plus any runs off the bat, and the ball is not counted. */
    public void addNoBall(int runsOffBat) {
        checkRuns(runsOffBat);
        ensureInPlay();
        runs += 1 + runsOffBat;
        extras++;
    }

    private void checkRuns(int r) {
        if (r < 0 || r > 7) {
            throw new IllegalArgumentException("Runs on a ball must be between 0 and 7");
        }
    }

    private void ensureInPlay() {
        if (isInningsOver()) {
            throw new IllegalStateException("The innings is already over");
        }
    }

    // ---------- state ----------

    public boolean isAllOut() {
        return wickets >= MAX_WICKETS;
    }

    public boolean isInningsOver() {
        return isAllOut() || balls >= MAX_BALLS;
    }

    /** Innings is over when all out, overs are done, or the chasing team reached its target. */
    public boolean isInningsOver(int target) {
        return isInningsOver() || (target > 0 && runs >= target);
    }

    public int getRuns() {
        return runs;
    }

    public int getWickets() {
        return wickets;
    }

    public int getBalls() {
        return balls;
    }

    public int getExtras() {
        return extras;
    }

    public String getOversString() {
        return (balls / 6) + "." + (balls % 6);
    }

    public double getRunRate() {
        return balls == 0 ? 0.0 : runs * 6.0 / balls;
    }

    /** Short form such as 200/10 (20.0 ov). */
    public String scoreLine() {
        return runs + "/" + wickets + " (" + getOversString() + " ov)";
    }

    // ---------- display ----------

    @Override
    public void displayScore() {
        displayTeam();
        System.out.println("Runs: " + runs);
        System.out.println("Wickets: " + wickets);
        System.out.println("Overs: " + getOversString());
        System.out.printf("Run Rate: %.2f%n", getRunRate());
        if (extras > 0) {
            System.out.println("Extras: " + extras);
        }
        System.out.println("----------------------");
    }

    public void displayScore(String title) {
        System.out.println("===== " + title + " =====");
        displayScore();
    }

    public static void displayMatchType() {
        System.out.println("Match Type: " + matchType);
    }

    @Override
    public void showMatchDetails() {
        System.out.println("Match Type: " + matchType);
    }
}
