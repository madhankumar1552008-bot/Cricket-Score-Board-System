package cricket;

/**
 * Base class for anything that belongs to a cricket match.
 * Holds the team name and forces subclasses to describe the match.
 */
public abstract class Match {
    protected String teamName;

    public Match(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be empty");
        }
        this.teamName = teamName.trim();
    }

    public String getTeamName() {
        return teamName;
    }

    public void displayTeam() {
        System.out.println("Team Name: " + teamName);
    }

    public abstract void showMatchDetails();
}
