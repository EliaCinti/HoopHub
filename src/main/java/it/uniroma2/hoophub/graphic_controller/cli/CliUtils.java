package it.uniroma2.hoophub.graphic_controller.cli;

import java.util.Scanner;

/**
 * Package-private utility class for CLI I/O operations.
 * <p>
 * This class centralizes all System.out and System.in usage for SonarQube compliance.
 * It is intentionally package-private to restrict access to CLI graphic controllers only.
 * </p>
 * <p>
 * <strong>Design:</strong> Only classes in the {@code graphic_controller.cli} package
 * should access this utility. Access is controlled via the abstract {@link CliGraphicController}.
 * </p>
 */
final class CliUtils {

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

    // ========== Output Methods ==========

    /**
     * Prints a regular message.
     */
    @SuppressWarnings("java:S106") // System.out is intentional for CLI output
    static void print(String message) {
        System.out.println(message);
    }

    /**
     * Prints a success message in green.
     */
    @SuppressWarnings("java:S106")
    static void printSuccess(String message) {
        System.out.println(ANSI_GREEN + SYMBOL_SUCCESS + message + ANSI_RESET);
    }

    /**
     * Prints an error message in red.
     */
    @SuppressWarnings("java:S106")
    static void printError(String message) {
        System.out.println(ANSI_RED + SYMBOL_ERROR + message + ANSI_RESET);
    }

    /**
     * Prints an info message in cyan.
     */
    @SuppressWarnings("java:S106")
    static void printInfo(String message) {
        System.out.println(ANSI_CYAN + SYMBOL_INFO + message + ANSI_RESET);
    }

    /**
     * Prints a warning message in yellow.
     */
    @SuppressWarnings("java:S106")
    static void printWarning(String message) {
        System.out.println(ANSI_YELLOW + SYMBOL_WARNING + message + ANSI_RESET);
    }

    /**
     * Prints a blank line.
     */
    @SuppressWarnings("java:S106")
    static void printNewLine() {
        System.out.println();
    }

    /**
     * Prints a horizontal separator line.
     */
    @SuppressWarnings("java:S106")
    static void printSeparator() {
        System.out.println(ANSI_CYAN + SEPARATOR_CHAR.repeat(SEPARATOR_LENGTH) + ANSI_RESET);
    }

    /**
     * Prints a title with decorative box border.
     */
    @SuppressWarnings("java:S106")
    static void printTitle(String title) {
        int boxWidth = title.length() + 4;
        String horizontalLine = BOX_HORIZONTAL.repeat(boxWidth - 2);

        System.out.println();
        System.out.println(formatBox(BOX_TOP_LEFT + horizontalLine + BOX_TOP_RIGHT));
        System.out.println(formatBox(BOX_VERTICAL + " " + title + " " + BOX_VERTICAL));
        System.out.println(formatBox(BOX_BOTTOM_LEFT + horizontalLine + BOX_BOTTOM_RIGHT));
        System.out.println();
    }

    /**
     * Prints a menu with numbered options.
     */
    @SuppressWarnings("java:S106")
    static void printMenu(String title, String... options) {
        System.out.println();
        System.out.println(formatBold(title));
        printSeparator();
        for (int i = 0; i < options.length; i++) {
            System.out.println(ANSI_YELLOW + (i + 1) + ". " + ANSI_RESET + options[i]);
        }
        printSeparator();
    }

    // ========== Input Methods ==========

    /**
     * Reads user input with a prompt.
     */
    @SuppressWarnings("java:S106")
    static String readInput(String prompt) {
        System.out.print(ANSI_BOLD + prompt + ANSI_RESET);
        System.out.flush();
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
