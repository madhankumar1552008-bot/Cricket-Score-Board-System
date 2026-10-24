package cricket;

/** The outcome of a two-innings match: who won and by how much. */
public class MatchResult {

    private final CricketScore first;
    private final CricketScore second;
    private final String winner;
    private final String description;

    private MatchResult(CricketScore first, CricketScore second, String winner, String description) {
        this.first = first;
        this.second = second;
        this.winner = winner;
        this.description = description;
    }

    /**
     * @param first  the team that batted first
     * @param second the team that chased
     */
    public static MatchResult decide(CricketScore first, CricketScore second) {
        if (second.getRuns() > first.getRuns()) {
            if (second.isAllOut()) {
                throw new IllegalArgumentException(
                        "The chasing team cannot win if it is all out");
            }
            int wicketsLeft = CricketScore.MAX_WICKETS - second.getWickets();
            int ballsLeft = CricketScore.MAX_BALLS - second.getBalls();
            String text = second.getTeamName() + " won by " + wicketsLeft
                    + (wicketsLeft == 1 ? " wicket" : " wickets")
                    + " (" + ballsLeft + (ballsLeft == 1 ? " ball" : " balls") + " left)";
            return new MatchResult(first, second, second.getTeamName(), text);
        }
        if (first.getRuns() > second.getRuns()) {
            int margin = first.getRuns() - second.getRuns();
            String text = first.getTeamName() + " won by " + margin
                    + (margin == 1 ? " run" : " runs");
            return new MatchResult(first, second, first.getTeamName(), text);
        }
        return new MatchResult(first, second, null, "Match tied");
    }

    public CricketScore getFirst() {
        return first;
    }

    public CricketScore getSecond() {
        return second;
    }

    /** Winning team name, or null if the match was tied. */
    public String getWinner() {
        return winner;
    }

    public boolean isTie() {
        return winner == null;
    }

    public String getDescription() {
        return description;
    }

    public void print() {
        System.out.println();
        first.showMatchDetails();
        first.displayScore("CRICKET SCOREBOARD");
        second.displayScore();
        System.out.println("RESULT: " + description);
    }
}
