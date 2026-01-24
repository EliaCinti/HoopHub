package it.uniroma2.hoophub.graphic_controller.cli;

import java.util.Scanner;

/**
 * Abstract base class for all CLI graphic controllers.
 *
 * <p>Provides console I/O operations with ANSI colors and formatting.
 * All CLI controllers must extend this class and implement {@link #execute()}.</p>
 *
 * <p>Design: Uses private static methods to encapsulate all System.out/in usage,
 * ensuring SonarQube compliance and consistent output formatting.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public abstract class CliGraphicController {

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

    /**
     * Executes the controller logic. Subclasses must implement specific behavior.
     */
    public abstract void execute();

    @SuppressWarnings("java:S106")
    private static final Scanner SCANNER = new Scanner(System.in);

    /**
     * Closes the shared scanner. Call when CLI application terminates.
     */
    public static void closeScanner() {
        SCANNER.close();
    }

    @SuppressWarnings("java:S106")
    private static void output(String message) {
        System.out.println(message);
    }

    @SuppressWarnings("java:S106")
    private static void outputNoNewline(String message) {
        System.out.print(message);
        System.out.flush();
    }

    // ========== Output Methods ==========

    /** Prints a regular message. */
    protected static void print(String message) {
        output(message);
    }

    /** Prints a success message in green with checkmark. */
    protected static void printSuccess(String message) {
        output(ANSI_GREEN + SYMBOL_SUCCESS + message + ANSI_RESET);
    }

    /** Prints an error message in red with X symbol. */
    protected static void printError(String message) {
        output(ANSI_RED + SYMBOL_ERROR + message + ANSI_RESET);
    }

    /** Prints an info message in cyan with info symbol. */
    protected static void printInfo(String message) {
        output(ANSI_CYAN + SYMBOL_INFO + message + ANSI_RESET);
    }

    /** Prints a warning message in yellow with warning symbol. */
    protected static void printWarning(String message) {
        output(ANSI_YELLOW + SYMBOL_WARNING + message + ANSI_RESET);
    }

    /** Prints a blank line. */
    protected static void printNewLine() {
        output("");
    }

    /** Clears the console screen by printing newlines. */
    protected static void clearScreen() {
        output("\n".repeat(50));
    }

    /** Prints a horizontal separator line. */
    protected static void printSeparator() {
        output(ANSI_CYAN + SEPARATOR_CHAR.repeat(SEPARATOR_LENGTH) + ANSI_RESET);
    }

    /** Prints a title with decorative box border. */
    protected static void printTitle(String title) {
        int boxWidth = title.length() + 4;
        String horizontalLine = BOX_HORIZONTAL.repeat(boxWidth - 2);

        printNewLine();
        output(formatBox(BOX_TOP_LEFT + horizontalLine + BOX_TOP_RIGHT));
        output(formatBox(BOX_VERTICAL + " " + title + " " + BOX_VERTICAL));
        output(formatBox(BOX_BOTTOM_LEFT + horizontalLine + BOX_BOTTOM_RIGHT));
        printNewLine();
    }

    /**
     * Prints a numbered menu with options.
     *
     * @param title   menu header
     * @param options menu items (1-indexed when displayed)
     */
    protected static void printMenu(String title, String... options) {
        printNewLine();
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
     *
     * @param prompt the prompt to display
     * @return trimmed user input
     */
    protected static String readInput(String prompt) {
        outputNoNewline(ANSI_BOLD + prompt + ANSI_RESET);
        return SCANNER.nextLine().trim();
    }

    // ========== Helper Methods ==========

    private static String formatBox(String text) {
        return ANSI_CYAN + ANSI_BOLD + text + ANSI_RESET;
    }

    private static String formatBold(String text) {
        return ANSI_BOLD + ANSI_CYAN + text + ANSI_RESET;
    }
}