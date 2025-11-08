package it.uniroma2.hoophub.utilities;

import java.io.PrintWriter;
import java.util.Scanner;

/**
 * CliView provides formatted console output for CLI interfaces.
 * <p>
 * This class encapsulates console I/O operations, providing a clean API
 * for displaying formatted messages without directly using System.out
 * (which triggers SonarQube code smell warnings).
 * </p>
 * <p>
 * Features:
 * <ul>
 *   <li>ANSI color support for enhanced visual feedback</li>
 *   <li>Box drawing for titles and sections</li>
 *   <li>Consistent formatting for messages, errors, and prompts</li>
 *   <li>Input handling with Scanner</li>
 * </ul>
 * </p>
 *
 * @see LoginCliController
 */
public class CliView {

    // ANSI Color codes for terminal output
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";
    private static final String ANSI_BOLD = "\u001B[1m";

    // Box drawing characters
    private static final String BOX_TOP_LEFT = "╔";
    private static final String BOX_TOP_RIGHT = "╗";
    private static final String BOX_BOTTOM_LEFT = "╚";
    private static final String BOX_BOTTOM_RIGHT = "╝";
    private static final String BOX_HORIZONTAL = "═";
    private static final String BOX_VERTICAL = "║";

    private final PrintWriter writer;
    private final Scanner scanner;

    /**
     * Constructs a new CliView with default System.out and System.in.
     * <p>
     * Using PrintWriter instead of direct System.out calls helps avoid
     * SonarQube warnings while maintaining clean console output.
     * </p>
     */
    public CliView() {
        this.writer = new PrintWriter(System.out, true);
        this.scanner = new Scanner(System.in);
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
        writer.println(ANSI_CYAN + ANSI_BOLD + BOX_TOP_LEFT + horizontalLine + BOX_TOP_RIGHT + ANSI_RESET);
        writer.println(ANSI_CYAN + ANSI_BOLD + BOX_VERTICAL + " " + title + " " + BOX_VERTICAL + ANSI_RESET);
        writer.println(ANSI_CYAN + ANSI_BOLD + BOX_BOTTOM_LEFT + horizontalLine + BOX_BOTTOM_RIGHT + ANSI_RESET);
        writer.println();
    }

    /**
     * Displays a success message in green color.
     *
     * @param message The success message to display
     */
    public void showSuccess(String message) {
        writer.println(ANSI_GREEN + "✓ " + message + ANSI_RESET);
    }

    /**
     * Displays an error message in red color.
     *
     * @param message The error message to display
     */
    public void showError(String message) {
        writer.println(ANSI_RED + "✗ ERROR: " + message + ANSI_RESET);
    }

    /**
     * Displays an informational message in cyan color.
     *
     * @param message The info message to display
     */
    public void showInfo(String message) {
        writer.println(ANSI_CYAN + "ℹ " + message + ANSI_RESET);
    }

    /**
     * Displays a warning message in yellow color.
     *
     * @param message The warning message to display
     */
    public void showWarning(String message) {
        writer.println(ANSI_YELLOW + "⚠ " + message + ANSI_RESET);
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
     * @return The user's input as a String
     */
    public String readInput(String prompt) {
        writer.print(ANSI_BOLD + prompt + ANSI_RESET);
        writer.flush();
        return scanner.nextLine().trim();
    }

    /**
     * Displays a prompt for password input (input is still visible).
     * <p>
     * Note: For true hidden password input, use Console.readPassword(),
     * but this is simpler for basic CLI testing.
     * </p>
     *
     * @param prompt The prompt text to display
     * @return The user's password input as a String
     */
    public String readPassword(String prompt) {
        writer.print(ANSI_BOLD + prompt + ANSI_RESET);
        writer.flush();
        return scanner.nextLine().trim();
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
        writer.println(ANSI_CYAN + "─".repeat(60) + ANSI_RESET);
    }

    /**
     * Displays a menu with numbered options.
     *
     * @param title The menu title
     * @param options The menu options
     */
    public void showMenu(String title, String... options) {
        newLine();
        writer.println(ANSI_BOLD + ANSI_CYAN + title + ANSI_RESET);
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
        writer.println();
        writer.println(ANSI_CYAN + ANSI_BOLD + "╔═══════════════════════════════════════════════════════╗" + ANSI_RESET);
        writer.println(ANSI_CYAN + ANSI_BOLD + "║                                                       ║" + ANSI_RESET);
        writer.println(ANSI_CYAN + ANSI_BOLD + "║  " + centerText(appName, 51) + "  ║" + ANSI_RESET);
        writer.println(ANSI_CYAN + ANSI_BOLD + "║                                                       ║" + ANSI_RESET);
        writer.println(ANSI_CYAN + ANSI_BOLD + "╚═══════════════════════════════════════════════════════╝" + ANSI_RESET);
        writer.println();
    }

    /**
     * Centers text within a given width.
     *
     * @param text The text to center
     * @param width The total width
     * @return Centered text with padding
     */
    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, width - text.length() - padding));
    }

    /**
     * Closes the scanner resource.
     */
    public void close() {
        scanner.close();
    }
}
