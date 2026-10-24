package cricket;

public class AustraliaScore extends CricketScore {

    public AustraliaScore() {
        super("Australia");
    }

    public AustraliaScore(int runs, int wickets, double overs) {
        super("Australia", runs, wickets, overs);
    }
}
