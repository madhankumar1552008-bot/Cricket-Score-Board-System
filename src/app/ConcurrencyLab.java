package app;

import java.util.concurrent.CountDownLatch;

/**
 * Two classic Java threading demonstrations, kept from the original lab:
 *
 *  1. wait / notify / notifyAll coordination between scoreboard threads
 *  2. a deliberate deadlock (using daemon threads so the program can still exit)
 */
final class ConcurrencyLab {

    private ConcurrencyLab() {
    }

    // ------------------------------------------------------------------
    // Demo 1: wait(), notify(), notifyAll()
    // ------------------------------------------------------------------

    static void runWaitNotifyDemo() throws InterruptedException {
        ThreadSignal signal = new ThreadSignal();
        ScoreboardThread india = new ScoreboardThread("India Thread", signal);
        ScoreboardThread australia = new ScoreboardThread("Australia Thread", signal);

        india.start();
        australia.start();
        signal.waitUntilReady(2);

        System.out.println("\nMain thread is notifying one thread...");
        signal.notifyOne();

        System.out.println("\nMain thread is notifying all threads...");
        signal.notifyEveryone();

        india.join();
        australia.join();
        System.out.println("\nWait/notify demo finished.");
    }

    /** Lets threads wait until the main thread releases them, one at a time or all at once. */
    static class ThreadSignal {
        private int ready = 0;
        private int permits = 0;
        private boolean releaseAll = false;

        synchronized void waitForSignal(String name) throws InterruptedException {
            ready++;
            System.out.println(name + " is waiting...");
            notifyAll();
            while (permits == 0 && !releaseAll) {
                wait();
            }
            if (releaseAll) {
                System.out.println(name + " received notifyAll().");
            } else {
                permits--;
                System.out.println(name + " received notify().");
            }
            notifyAll();
        }

        synchronized void waitUntilReady(int count) throws InterruptedException {
            while (ready < count) {
                wait();
            }
        }

        synchronized void notifyOne() throws InterruptedException {
            permits++;
            notify();
            while (permits > 0) {
                wait();
            }
        }

        synchronized void notifyEveryone() {
            releaseAll = true;
            notifyAll();
        }
    }

    static class ScoreboardThread extends Thread {
        private final String name;
        private final ThreadSignal signal;

        ScoreboardThread(String name, ThreadSignal signal) {
            this.name = name;
            this.signal = signal;
        }

        @Override
        public void run() {
            try {
                System.out.println(name + " is preparing...");
                Thread.sleep(500);
                signal.waitForSignal(name);
            } catch (InterruptedException e) {
                System.out.println(name + " was interrupted.");
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------------------------------------------------------
    // Demo 2: deadlock
    // ------------------------------------------------------------------

    static void runDeadlockDemo() throws InterruptedException {
        System.out.println("\nStarting deadlock demonstration...");
        Object indiaLock = new Object();
        Object australiaLock = new Object();
        CountDownLatch locksHeld = new CountDownLatch(2);

        DeadlockThread t1 = new DeadlockThread("India Deadlock Thread", indiaLock, australiaLock, locksHeld);
        DeadlockThread t2 = new DeadlockThread("Australia Deadlock Thread", australiaLock, indiaLock, locksHeld);

        // Daemon threads: they stay stuck forever, but will not keep the JVM alive.
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        locksHeld.await();
        System.out.println("Both threads hold one lock and are attempting to acquire the other's lock.");
        Thread.sleep(300);
        System.out.println("Deadlock demonstrated. Daemon threads allow the main program to finish.");
    }

    static class DeadlockThread extends Thread {
        private final Object firstLock;
        private final Object secondLock;
        private final CountDownLatch locksHeld;

        DeadlockThread(String name, Object firstLock, Object secondLock, CountDownLatch locksHeld) {
            super(name);
            this.firstLock = firstLock;
            this.secondLock = secondLock;
            this.locksHeld = locksHeld;
        }

        @Override
        public void run() {
            synchronized (firstLock) {
                System.out.println(getName() + " acquired its first lock.");
                locksHeld.countDown();
                try {
                    locksHeld.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                System.out.println(getName() + " is waiting for its second lock.");
                synchronized (secondLock) {
                    System.out.println(getName() + " acquired both locks.");
                }
            }
        }
    }
}
