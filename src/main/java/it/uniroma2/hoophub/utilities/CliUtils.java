package it.uniroma2.hoophub.utilities;

import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Utility class providing CLI-related helper methods and constants.
 * <p>
 * This class centralizes System.out/in usage to a single point for SonarQube compliance,
 * and provides ANSI color codes and other CLI utilities for boundary classes.
 * </p>
 */
public final class CliUtils {

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

    /** Cross/X symbol */
    public static final String SYMBOL_CROSS = "✗";

    /** Info symbol */
    public static final String SYMBOL_INFO = "ℹ";

    /** Warning symbol */
    public static final String SYMBOL_WARNING = "⚠";

    /** Arrow right symbol */
    public static final String SYMBOL_ARROW_RIGHT = "→";

    /** Bullet point symbol */
    public static final String SYMBOL_BULLET = "•";

    // ========== Private Constructor ==========

    private CliUtils() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== Factory Method for CliView ==========

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

    // ========== Helper Methods ==========

    /**
     * Clears the console screen (platform-dependent).
     * Works on most Unix/Linux terminals and Windows 10+ with ANSI support.
     */
    public static void clearScreen() {
        createStandardCliView().showMessage("\033[H\033[2J");
        System.out.flush();
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
