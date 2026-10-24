package cricket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Estimates win chances by playing the same fixture many times.
 * The matches run in parallel on a thread pool, one simulator per match,
 * so no state is shared between threads.
 */
public class WinPredictor {

    /** Aggregated numbers from a batch of simulated matches. */
    public static class Prediction {
        public final String teamA;
        public final String teamB;
        public final int winsA;
        public final int winsB;
        public final int ties;
        public final int total;
        public final double averageA;
        public final double averageB;

        Prediction(String teamA, String teamB, int winsA, int winsB, int ties,
                   int total, double averageA, double averageB) {
            this.teamA = teamA;
            this.teamB = teamB;
            this.winsA = winsA;
            this.winsB = winsB;
            this.ties = ties;
            this.total = total;
            this.averageA = averageA;
            this.averageB = averageB;
        }

        public double percentA() {
            return winsA * 100.0 / total;
        }

        public double percentB() {
            return winsB * 100.0 / total;
        }

        public double percentTies() {
            return ties * 100.0 / total;
        }
    }

    public static Prediction run(String teamA, double ratingA, String teamB, double ratingB,
                                 int simulations, long seed, int threads)
            throws InterruptedException, ExecutionException {

        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, threads));
        try {
            List<Future<MatchResult>> futures = new ArrayList<>();
            for (int i = 0; i < simulations; i++) {
                final long matchSeed = seed + i;
                Callable<MatchResult> job = () ->
                        new MatchSimulator(matchSeed, false)
                                .simulateMatch(teamA, teamB, ratingA, ratingB);
                futures.add(pool.submit(job));
            }

            int winsA = 0;
            int winsB = 0;
            int ties = 0;
            long runsA = 0;
            long runsB = 0;

            for (Future<MatchResult> future : futures) {
                MatchResult result = future.get();
                if (result.isTie()) {
                    ties++;
                } else if (result.getWinner().equals(teamA)) {
                    winsA++;
                } else {
                    winsB++;
                }
                runsA += runsFor(result, teamA);
                runsB += runsFor(result, teamB);
            }

            return new Prediction(teamA, teamB, winsA, winsB, ties, simulations,
                    runsA / (double) simulations, runsB / (double) simulations);
        } finally {
            pool.shutdown();
        }
    }

    private static int runsFor(MatchResult result, String team) {
        return result.getFirst().getTeamName().equals(team)
                ? result.getFirst().getRuns()
                : result.getSecond().getRuns();
    }
}
