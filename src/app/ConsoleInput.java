package app;

import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Small wrapper around Scanner that re-asks until the user types something valid.
 * Throws NoSuchElementException when the input stream is closed.
 */
final class ConsoleInput {

    private final Scanner scanner;

    ConsoleInput(InputStream in) {
        this.scanner = new Scanner(in);
    }

    String line(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException("Input closed");
        }
        return scanner.nextLine().trim();
    }

    int integer(String prompt, int min, int max) {
        while (true) {
            String text = line(prompt + " [" + min + "-" + max + "]: ");
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("Please enter a whole number from " + min + " to " + max + ".");
        }
    }

    double decimal(String prompt, double min, double max) {
        while (true) {
            String text = line(prompt + ": ");
            try {
                double value = Double.parseDouble(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("Please enter a number from " + min + " to " + max + ".");
        }
    }

    /** Returns the default when the user just presses Enter. */
    double decimalOrDefault(String prompt, double min, double max, double fallback) {
        while (true) {
            String text = line(prompt + " (Enter for " + fallback + "): ");
            if (text.isEmpty()) {
                return fallback;
            }
            try {
                double value = Double.parseDouble(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("Please enter a number from " + min + " to " + max + ".");
        }
    }

    int integerOrDefault(String prompt, int min, int max, int fallback) {
        while (true) {
            String text = line(prompt + " (Enter for " + fallback + "): ");
            if (text.isEmpty()) {
                return fallback;
            }
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("Please enter a whole number from " + min + " to " + max + ".");
        }
    }

    long longOrDefault(String prompt, long fallback) {
        while (true) {
            String text = line(prompt);
            if (text.isEmpty()) {
                return fallback;
            }
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number, or press Enter to skip.");
            }
        }
    }

    String nameOrDefault(String prompt, String fallback) {
        String text = line(prompt + " (Enter for " + fallback + "): ");
        return text.isEmpty() ? fallback : text;
    }
}
