package cricket;

/**
 * Picks the right CricketScore subclass for a team name, so India and
 * Australia keep their special behaviour and any other team still works.
 */
public final class ScoreFactory {

    private ScoreFactory() {
    }

    public static CricketScore create(String teamName) {
        if ("India".equalsIgnoreCase(teamName.trim())) {
            return new IndiaScore();
        }
        if ("Australia".equalsIgnoreCase(teamName.trim())) {
            return new AustraliaScore();
        }
        return new CricketScore(teamName);
    }

    public static CricketScore create(String teamName, int runs, int wickets, double overs) {
        if ("India".equalsIgnoreCase(teamName.trim())) {
            return new IndiaScore(runs, wickets, overs);
        }
        if ("Australia".equalsIgnoreCase(teamName.trim())) {
            return new AustraliaScore(runs, wickets, overs);
        }
        return new CricketScore(teamName, runs, wickets, overs);
    }
}
