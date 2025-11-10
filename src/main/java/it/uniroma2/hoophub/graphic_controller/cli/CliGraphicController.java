package it.uniroma2.hoophub.graphic_controller.cli;

import java.util.Scanner;

/**
 * Abstract base class for all CLI graphic controllers.
 * <p>
 * This class defines the common contract for CLI boundary classes and provides
 * protected methods for console I/O operations. All CLI graphic controllers must
 * extend this class and implement the {@link #execute()} method.
 * </p>
 * <p>
 * <strong>Design:</strong> CliUtils is a private inner class that centralizes all
 * System.out/in usage. Only this abstract class can access CliUtils, ensuring complete
 * encapsulation and SonarQube compliance.
 * </p>
 */
public abstract class CliGraphicController {

    /**
     * Executes the graphic controller logic.
     * Subclasses must implement this method to define their specific behavior.
     */
    public abstract void execute();

    // ========== Protected I/O Methods ==========

    /**
     * Displays a regular message.
     */
    protected void print(String message) {
        CliUtils.print(message);
    }

    /**
     * Displays a success message in green color.
     */
    protected void printSuccess(String message) {
        CliUtils.printSuccess(message);
    }

    /**
     * Displays an error message in red color.
     */
    protected void printError(String message) {
        CliUtils.printError(message);
    }

    /**
     * Displays an informational message in cyan color.
     */
    protected void printInfo(String message) {
        CliUtils.printInfo(message);
    }

    /**
     * Displays a warning message in yellow color.
     */
    protected void printWarning(String message) {
        CliUtils.printWarning(message);
    }

    /**
     * Prints a blank line for spacing.
     */
    protected void printNewLine() {
        CliUtils.printNewLine();
    }

    /**
     * Prints a horizontal separator line.
     */
    protected void printSeparator() {
        CliUtils.printSeparator();
    }

    /**
     * Displays a title with a decorative box border.
     */
    protected void printTitle(String title) {
        CliUtils.printTitle(title);
    }

    /**
     * Displays a menu with numbered options.
     */
    protected void printMenu(String title, String... options) {
        CliUtils.printMenu(title, options);
    }

    /**
     * Displays a prompt and reads user input.
     */
    protected String readInput(String prompt) {
        return CliUtils.readInput(prompt);
    }

    /**
     * Displays a prompt for password input.
     * Note: Input is still visible in basic CLI. For hidden input, use Console.readPassword().
     */
    protected String readPassword(String prompt) {
        return CliUtils.readPassword(prompt);
    }

    // ========== Public Static Method for Cleanup ==========

    /**
     * Closes the scanner resource.
     * Should be called when the CLI application terminates.
     */
    public static void closeScanner() {
        CliUtils.closeScanner();
    }

    // ========== Private Inner Class: CliUtils ==========

    /**
     * Private utility class for CLI I/O operations.
     * <p>
     * This class centralizes all System.out and System.in usage for SonarQube compliance.
     * It is intentionally private to ensure complete encapsulation - only the outer
     * CliGraphicController class can access these methods.
     * </p>
     * <p>
     * <strong>Design:</strong> Only two methods contain direct System.out references:
     * {@code output()} and {@code outputNoNewline()}. All other methods use these base methods.
     * </p>
     */
    private static final class CliUtils {

        // ========== ANSI Color Codes ==========

        private static final String ANSI_RESET = "\u001B[0m";
        private static final String ANSI_RED = "\u001B[31m";
        private static final String ANSI_GREEN = "\u001B[32m";
        private static final String ANSI_YELLOW = "\u001B[33m";
        private static final String ANSI_CYAN = "\u001B[36m";
        private static final String ANSI_BOLD = "\u001B[1m";

        // ========== Box Drawing Characters ==========

        private static final String BOX_TOP_LEFT = "╔";
        private static final String BOX_TOP_RIGHT = "╗";
        private static final String BOX_BOTTOM_LEFT = "╚";
        private static final String BOX_BOTTOM_RIGHT = "╝";
        private static final String BOX_HORIZONTAL = "═";
        private static final String BOX_VERTICAL = "║";

        // ========== Symbols ==========

        private static final String SYMBOL_SUCCESS = "✓ ";
        private static final String SYMBOL_ERROR = "✗ ERROR: ";
        private static final String SYMBOL_INFO = "ℹ ";
        private static final String SYMBOL_WARNING = "⚠ ";

        // ========== Formatting ==========

        private static final int SEPARATOR_LENGTH = 60;
        private static final String SEPARATOR_CHAR = "─";

        // ========== Scanner (Singleton for input) ==========

        @SuppressWarnings("java:S106") // System.in is necessary for CLI input
        private static final Scanner SCANNER = new Scanner(System.in);

        // ========== Constructor ==========

        private CliUtils() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }

        // ========== Base Output Methods (ONLY System.out references) ==========

        /**
         * Base output method with newline.
         * This is the ONLY method that uses System.out.println.
         * All other output methods must use this method.
         */
        @SuppressWarnings("java:S106") // System.out is intentional for CLI output
        private static void output(String message) {
            System.out.println(message);
        }

        /**
         * Base output method without newline.
         * This is the ONLY method that uses System.out.print.
         * Used for prompts and partial output.
         */
        @SuppressWarnings("java:S106") // System.out is intentional for CLI output
        private static void outputNoNewline(String message) {
            System.out.print(message);
            System.out.flush();
        }

        // ========== Public Output Methods (use base methods) ==========

        /**
         * Prints a regular message.
         */
        static void print(String message) {
            output(message);
        }

        /**
         * Prints a success message in green.
         */
        static void printSuccess(String message) {
            output(ANSI_GREEN + SYMBOL_SUCCESS + message + ANSI_RESET);
        }

        /**
         * Prints an error message in red.
         */
        static void printError(String message) {
            output(ANSI_RED + SYMBOL_ERROR + message + ANSI_RESET);
        }

        /**
         * Prints an info message in cyan.
         */
        static void printInfo(String message) {
            output(ANSI_CYAN + SYMBOL_INFO + message + ANSI_RESET);
        }

        /**
         * Prints a warning message in yellow.
         */
        static void printWarning(String message) {
            output(ANSI_YELLOW + SYMBOL_WARNING + message + ANSI_RESET);
        }

        /**
         * Prints a blank line.
         */
        static void printNewLine() {
            output("");
        }

        /**
         * Prints a horizontal separator line.
         */
        static void printSeparator() {
            output(ANSI_CYAN + SEPARATOR_CHAR.repeat(SEPARATOR_LENGTH) + ANSI_RESET);
        }

        /**
         * Prints a title with decorative box border.
         */
        static void printTitle(String title) {
            int boxWidth = title.length() + 4;
            String horizontalLine = BOX_HORIZONTAL.repeat(boxWidth - 2);

            output("");
            output(formatBox(BOX_TOP_LEFT + horizontalLine + BOX_TOP_RIGHT));
            output(formatBox(BOX_VERTICAL + " " + title + " " + BOX_VERTICAL));
            output(formatBox(BOX_BOTTOM_LEFT + horizontalLine + BOX_BOTTOM_RIGHT));
            output("");
        }

        /**
         * Prints a menu with numbered options.
         */
        static void printMenu(String title, String... options) {
            output("");
            output(formatBold(title));
            printSeparator();
            for (int i = 0; i < options.length; i++) {
                output(ANSI_YELLOW + (i + 1) + ". " + ANSI_RESET + options[i]);
            }
            printSeparator();
        }

        // ========== Input Methods ==========

        /**
         * Reads user input with a prompt.
         */
        static String readInput(String prompt) {
            outputNoNewline(ANSI_BOLD + prompt + ANSI_RESET);
            return SCANNER.nextLine().trim();
        }

        /**
         * Reads password input with a prompt.
         * Note: Input is still visible in basic CLI. For hidden input, use Console.readPassword().
         */
        static String readPassword(String prompt) {
            return readInput(prompt);
        }

        // ========== Helper Methods ==========

        /**
         * Formats text with box style (cyan and bold).
         */
        private static String formatBox(String text) {
            return ANSI_CYAN + ANSI_BOLD + text + ANSI_RESET;
        }

        /**
         * Formats text with bold style.
         */
        private static String formatBold(String text) {
            return ANSI_BOLD + ANSI_CYAN + text + ANSI_RESET;
        }

        /**
         * Closes the scanner resource.
         * Should be called when the CLI application terminates.
         */
        static void closeScanner() {
            SCANNER.close();
        }
    }
}
