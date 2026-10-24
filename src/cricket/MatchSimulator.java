package cricket;

import java.util.Random;

/**
 * Simulates T20 innings ball by ball.
 *
 * Every team has a strength rating (1.0 is average). A higher rating gives
 * more boundaries and fewer wickets. Using the same seed always replays the
 * same match, which makes results easy to test and share.
 */
public class MatchSimulator {

    private final Random rng;
    private final boolean commentary;

    public MatchSimulator(long seed, boolean commentary) {
        this.rng = new Random(seed);
        this.commentary = commentary;
    }

    /** Plays a full match; the toss decides who bats first. */
    public MatchResult simulateMatch(String teamA, String teamB, double ratingA, double ratingB) {
        boolean aBatsFirst = rng.nextBoolean();
        String firstName = aBatsFirst ? teamA : teamB;
        String secondName = aBatsFirst ? teamB : teamA;
        double firstRating = aBatsFirst ? ratingA : ratingB;
        double secondRating = aBatsFirst ? ratingB : ratingA;

        if (commentary) {
            System.out.println("Toss: " + firstName + " won and chose to bat first.");
            System.out.println();
            System.out.println("--- 1st innings: " + firstName + " ---");
        }
        CricketScore first = ScoreFactory.create(firstName);
        simulateInnings(first, 0, firstRating);

        int target = first.getRuns() + 1;
        if (commentary) {
            System.out.println();
            System.out.println("--- 2nd innings: " + secondName + " need " + target + " to win ---");
        }
        CricketScore second = ScoreFactory.create(secondName);
        simulateInnings(second, target, secondRating);

        return MatchResult.decide(first, second);
    }

    /**
     * Bats one innings until it ends.
     *
     * @param target runs needed to win, or 0 for a first innings
     */
    public void simulateInnings(CricketScore score, int target, double rating) {
        int runsAtOverStart = score.getRuns();

        while (!score.isInningsOver(target)) {
            int ballsBefore = score.getBalls();
            String event = playOneBall(score, rating);

            if (commentary && event != null) {
                System.out.println("  " + score.getOversString() + "  " + event
                        + "  -> " + score.getRuns() + "/" + score.getWickets());
            }

            boolean overCompleted = score.getBalls() != ballsBefore && score.getBalls() % 6 == 0;
            if (overCompleted) {
                if (commentary) {
                    System.out.printf("End of over %d: %d/%d  (%d runs in the over, run rate %.2f)%n",
                            score.getBalls() / 6, score.getRuns(), score.getWickets(),
                            score.getRuns() - runsAtOverStart, score.getRunRate());
                }
                runsAtOverStart = score.getRuns();
            }
        }
    }

    /** Plays a single delivery and returns a short note for notable events, else null. */
    private String playOneBall(CricketScore score, double rating) {
        double wide = 3;
        double noBall = 1;
        double wicket = 6.0 / rating;
        double six = 5.0 * rating;
        double four = 11.0 * rating;
        double three = 1;
        double two = 8;
        double one = 30;

        double roll = rng.nextDouble() * 100;
        double limit = wide;
        if (roll < limit) {
            score.addWide();
            return null;
        }
        limit += noBall;
        if (roll < limit) {
            score.addNoBall(0);
            return null;
        }
        limit += wicket;
        if (roll < limit) {
            score.addWicket(0);
            return "WICKET!";
        }
        limit += six;
        if (roll < limit) {
            score.addRuns(6);
            return "SIX!";
        }
        limit += four;
        if (roll < limit) {
            score.addRuns(4);
            return "FOUR!";
        }
        limit += three;
        if (roll < limit) {
            score.addRuns(3);
            return null;
        }
        limit += two;
        if (roll < limit) {
            score.addRuns(2);
            return null;
        }
        limit += one;
        if (roll < limit) {
            score.addRuns(1);
            return null;
        }
        score.addRuns(0);
        return null;
    }
}
