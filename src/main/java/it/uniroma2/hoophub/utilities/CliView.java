package it.uniroma2.hoophub.utilities;

import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Unified utility class for CLI interfaces providing formatted console output and helper methods.
 * <p>
 * This class centralizes System.out/in usage to a single point for SonarQube compliance,
 * and provides ANSI color codes, formatting utilities, and view methods for boundary classes.
 * Uses dependency injection to avoid direct System.out references throughout the codebase.
 * </p>
 */
public class CliView {

    // ========== ANSI Color Codes (Public for CLI boundaries) ==========

    /** ANSI reset code - resets all formatting */
    public static final String ANSI_RESET = "\u001B[0m";

    /** ANSI red color */
    public static final String ANSI_RED = "\u001B[31m";

    /** ANSI green color */
    public static final String ANSI_GREEN = "\u001B[32m";

    /** ANSI yellow color */
    public static final String ANSI_YELLOW = "\u001B[33m";

    /** ANSI blue color */
    public static final String ANSI_BLUE = "\u001B[34m";

    /** ANSI magenta color */
    public static final String ANSI_MAGENTA = "\u001B[35m";

    /** ANSI cyan color */
    public static final String ANSI_CYAN = "\u001B[36m";

    /** ANSI white color */
    public static final String ANSI_WHITE = "\u001B[37m";

    /** ANSI bold style */
    public static final String ANSI_BOLD = "\u001B[1m";

    /** ANSI underline style */
    public static final String ANSI_UNDERLINE = "\u001B[4m";

    // ========== Box Drawing Characters ==========

    /** Box drawing: top-left corner */
    public static final String BOX_TOP_LEFT = "╔";

    /** Box drawing: top-right corner */
    public static final String BOX_TOP_RIGHT = "╗";

    /** Box drawing: bottom-left corner */
    public static final String BOX_BOTTOM_LEFT = "╚";

    /** Box drawing: bottom-right corner */
    public static final String BOX_BOTTOM_RIGHT = "╝";

    /** Box drawing: horizontal line */
    public static final String BOX_HORIZONTAL = "═";

    /** Box drawing: vertical line */
    public static final String BOX_VERTICAL = "║";

    // ========== Unicode Symbols ==========

    /** Checkmark symbol */
    public static final String SYMBOL_CHECK = "✓";

    /** Success symbol with space */
    public static final String SYMBOL_SUCCESS = "✓ ";

    /** Cross/X symbol */
    public static final String SYMBOL_CROSS = "✗";

    /** Error symbol with text */
    public static final String SYMBOL_ERROR = "✗ ERROR: ";

    /** Info symbol */
    public static final String SYMBOL_INFO = "ℹ ";

    /** Warning symbol */
    public static final String SYMBOL_WARNING = "⚠ ";

    /** Arrow right symbol */
    public static final String SYMBOL_ARROW_RIGHT = "→";

    /** Bullet point symbol */
    public static final String SYMBOL_BULLET = "•";

    // ========== Formatting Constants ==========

    /** Default separator length */
    public static final int SEPARATOR_LENGTH = 60;

    /** Default separator character */
    public static final String SEPARATOR_CHAR = "─";

    /** Default banner width */
    public static final int BANNER_WIDTH = 51;

    private final PrintWriter writer;
    private final Scanner scanner;

    /**
     * Constructs a new CliView with injected streams.
     * NO direct System.out reference - avoids SonarQube code smell.
     *
     * @param writer The PrintWriter for output
     * @param scanner The Scanner for input
     */
    public CliView(PrintWriter writer, Scanner scanner) {
        this.writer = writer;
        this.scanner = scanner;
    }

    // ========== Factory Method ==========

    /**
     * Creates a standard CliView instance using System.out and System.in.
     * <p>
     * This is the ONLY place in the application where System.out is referenced directly,
     * centralizing console I/O for better testability and SonarQube compliance.
     * </p>
     *
     * @return A new CliView instance for console I/O
     */
    @SuppressWarnings("java:S106") // System.out is intentional and necessary for CLI output
    public static CliView createStandardCliView() {
        PrintWriter writer = new PrintWriter(System.out, true);
        Scanner scanner = new Scanner(System.in);
        return new CliView(writer, scanner);
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

    // ========== Static Helper Methods ==========

    /**
     * Clears the console screen using ANSI escape codes.
     * Works on most Unix/Linux terminals and Windows 10+ with ANSI support.
     * Uses the writer to avoid direct System.out references.
     */
    public void clearScreen() {
        writer.print("\033[H\033[2J");
        writer.flush();
    }

    /**
     * Colorizes text with the specified ANSI color code.
     *
     * @param text The text to colorize
     * @param colorCode The ANSI color code
     * @return The colorized text with reset appended
     */
    public static String colorize(String text, String colorCode) {
        return colorCode + text + ANSI_RESET;
    }

    /**
     * Makes text bold using ANSI formatting.
     *
     * @param text The text to make bold
     * @return The bold text with reset appended
     */
    public static String bold(String text) {
        return ANSI_BOLD + text + ANSI_RESET;
    }

    /**
     * Centers text within a given width by adding padding.
     *
     * @param text The text to center
     * @param width The total width
     * @return The centered text with padding
     */
    public static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        int rightPadding = width - text.length() - padding;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, rightPadding));
    }

    /**
     * Creates a horizontal line separator of the specified length.
     *
     * @param length The length of the separator
     * @param character The character to use for the separator
     * @return The separator string
     */
    public static String createSeparator(int length, String character) {
        return character.repeat(length);
    }
}
