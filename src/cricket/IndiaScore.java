package cricket;

public class IndiaScore extends CricketScore {

    public IndiaScore() {
        super("India");
    }

    public IndiaScore(int runs, int wickets, double overs) {
        super("India", runs, wickets, overs);
    }

    @Override
    public void displayTeam() {
        System.out.println("Team Name: " + teamName + " (Home Team)");
    }
}
