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
     * Executes the graphic controller logic.
     * Subclasses must implement this method to define their specific behavior.
     */
    public abstract void execute();



    // ========== Scanner (Singleton for input) ==========

    @SuppressWarnings("java:S106") // System.in is necessary for CLI input
    private static final Scanner SCANNER = new Scanner(System.in);

    /**
     * Closes the scanner resource.
     * Should be called when the CLI application terminates.
     */
    public static void closeScanner() {
        SCANNER.close();
    }

    /**
     * Base output method with newline.
     * This is the ONLY method that uses System.out.println.
     * All other output methods must use this method.
     */
    @SuppressWarnings("java:S106") // System.out is intentional for CLI output
    private static void output(String message) {
        System.out.println(message);
    }

    // ========== Public Output Methods ==========

    /**
     * Prints a regular message.
     */
    protected static void print(String message) {
        output(message);
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

    /**
     * Prints a success message in green.
     */
    protected static void printSuccess(String message) {
        output(ANSI_GREEN + SYMBOL_SUCCESS + message + ANSI_RESET);
    }

    /**
     * Prints an error message in red.
     */
    protected static void printError(String message) {
        output(ANSI_RED + SYMBOL_ERROR + message + ANSI_RESET);
    }

    /**
     * Prints an info message in cyan.
     */
    protected static void printInfo(String message) {
        output(ANSI_CYAN + SYMBOL_INFO + message + ANSI_RESET);
    }

    /**
     * Prints a warning message in yellow.
     */
    protected static void printWarning(String message) {
        output(ANSI_YELLOW + SYMBOL_WARNING + message + ANSI_RESET);
    }

    /**
     * Prints a blank line.
     */
    protected static void printNewLine() {
        output("");
    }

    /**
     * Clears the console screen using ANSI escape codes.
     */
    protected static void clearScreen() {
        output("\u001B[2J\u001B[H");
        System.out.flush();
    }

    /**
     * Prints a horizontal separator line.
     */
    protected static void printSeparator() {
        output(ANSI_CYAN + SEPARATOR_CHAR.repeat(SEPARATOR_LENGTH) + ANSI_RESET);
    }

    /**
     * Prints a title with decorative box border.
     */
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
     * Prints a menu with numbered options.
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
     */
    protected static String readInput(String prompt) {
        outputNoNewline(ANSI_BOLD + prompt + ANSI_RESET);
        return SCANNER.nextLine().trim();
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
}

