package it.uniroma2.hoophub.utilities;

import java.io.PrintWriter;
import java.util.Scanner;

/**
 * CliView provides formatted console output for CLI interfaces.
 * Uses dependency injection to avoid direct System. Out references (SonarQube code smell).
 */
public class CliView {

    // ANSI Color codes
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_BOLD = "\u001B[1m";

    // Box drawing characters
    private static final String BOX_TOP_LEFT = "╔";
    private static final String BOX_TOP_RIGHT = "╗";
    private static final String BOX_BOTTOM_LEFT = "╚";
    private static final String BOX_BOTTOM_RIGHT = "╝";
    private static final String BOX_HORIZONTAL = "═";
    private static final String BOX_VERTICAL = "║";

    // Symbols
    private static final String SYMBOL_SUCCESS = "✓ ";
    private static final String SYMBOL_ERROR = "✗ ERROR: ";
    private static final String SYMBOL_INFO = "ℹ ";
    private static final String SYMBOL_WARNING = "⚠ ";

    // Formatting
    private static final int SEPARATOR_LENGTH = 60;
    private static final String SEPARATOR_CHAR = "─";
    private static final int BANNER_WIDTH = 51;

    private final PrintWriter writer;
    private final Scanner scanner;

    /**
     * Constructs a new CliView with injected streams.
     * NO direct System. Out reference - avoids SonarQube code smell.
     *
     * @param writer The PrintWriter for output
     * @param scanner The Scanner for input
     */
    public CliView(PrintWriter writer, Scanner scanner) {
        this.writer = writer;
        this.scanner = scanner;
    }

    /**
     * Displays a title with a decorative box border.
     *
     * @param title The title text to display
     */
    public void showTitle(String title) {
        int boxWidth = title.length() + 4;
        String horizontalLine = BOX_HORIZONTAL.repeat(boxWidth - 2);

        writer.println();
        writer.println(formatBox(BOX_TOP_LEFT + horizontalLine + BOX_TOP_RIGHT));
        writer.println(formatBox(BOX_VERTICAL + " " + title + " " + BOX_VERTICAL));
        writer.println(formatBox(BOX_BOTTOM_LEFT + horizontalLine + BOX_BOTTOM_RIGHT));
        writer.println();
    }

    /**
     * Displays a success message in green color.
     *
     * @param message The success message to display
     */
    public void showSuccess(String message) {
        writer.println(ANSI_GREEN + SYMBOL_SUCCESS + message + ANSI_RESET);
    }

    /**
     * Displays an error message in red color.
     *
     * @param message The error message to display
     */
    public void showError(String message) {
        writer.println(ANSI_RED + SYMBOL_ERROR + message + ANSI_RESET);
    }

    /**
     * Displays an informational message in cyan color.
     *
     * @param message The info message to display
     */
    public void showInfo(String message) {
        writer.println(ANSI_CYAN + SYMBOL_INFO + message + ANSI_RESET);
    }

    /**
     * Displays a warning message in yellow color.
     *
     * @param message The warning message to display
     */
    public void showWarning(String message) {
        writer.println(ANSI_YELLOW + SYMBOL_WARNING + message + ANSI_RESET);
    }

    /**
     * Displays a regular message without color.
     *
     * @param message The message to display
     */
    public void showMessage(String message) {
        writer.println(message);
    }

    /**
     * Displays a prompt and reads user input.
     *
     * @param prompt The prompt text to display
     * @return The user's input as a String, trimmed
     */
    public String readInput(String prompt) {
        writer.print(ANSI_BOLD + prompt + ANSI_RESET);
        writer.flush();
        return scanner.nextLine().trim();
    }

    /**
     * Displays a prompt for password input.
     * Note: Input is still visible in basic CLI. For hidden input, use Console.readPassword().
     *
     * @param prompt The prompt text to display
     * @return The user's password input as a String, trimmed
     */
    public String readPassword(String prompt) {
        return readInput(prompt);
    }

    /**
     * Prints a blank line for spacing.
     */
    public void newLine() {
        writer.println();
    }

    /**
     * Prints a horizontal separator line.
     */
    public void showSeparator() {
        writer.println(ANSI_CYAN + SEPARATOR_CHAR.repeat(SEPARATOR_LENGTH) + ANSI_RESET);
    }

    /**
     * Displays a menu with numbered options.
     *
     * @param title The menu title
     * @param options The menu options (variable arguments)
     */
    public void showMenu(String title, String... options) {
        newLine();
        writer.println(formatBold(title));
        showSeparator();
        for (int i = 0; i < options.length; i++) {
            writer.println(ANSI_YELLOW + (i + 1) + ". " + ANSI_RESET + options[i]);
        }
        showSeparator();
    }

    /**
     * Displays the application banner/logo.
     *
     * @param appName The application name
     */
    public void showBanner(String appName) {
        String banner = "╔═══════════════════════════════════════════════════════╗";
        String emptyLine = "║                                                       ║";

        writer.println();
        writer.println(formatBox(banner));
        writer.println(formatBox(emptyLine));
        writer.println(formatBox("║  " + centerText(appName) + "║"));
        writer.println(formatBox(emptyLine));
        writer.println(formatBox("╚═══════════════════════════════════════════════════════╝"));
        writer.println();
    }

    /**
     * Formats text with box style (cyan and bold).
     */
    private String formatBox(String text) {
        return ANSI_CYAN + ANSI_BOLD + text + ANSI_RESET;
    }

    /**
     * Formats text with bold style.
     */
    private String formatBold(String text) {
        return ANSI_BOLD + ANSI_CYAN + text + ANSI_RESET;
    }

    /**
     * Centers text within a given width.
     *
     * @param text The text to center
     * @return Centered text with padding
     */
    private String centerText(String text) {
        int padding = (CliView.BANNER_WIDTH - text.length()) / 2;
        int rightPadding = CliView.BANNER_WIDTH - text.length() - padding;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, rightPadding));
    }

    /**
     * Closes the scanner resource.
     * Should be called when the CLI application terminates.
     */
    public void close() {
        scanner.close();
    }
}
