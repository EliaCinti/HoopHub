package it.uniroma2.hoophub.utilities;

import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Factory for creating CliView instances.
 * Centralizes System. out usage to a single point for SonarQube compliance.
 */
public final class CliViewFactory {

    private CliViewFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a standard CliView instance using System. out and System.in.
     * This is the ONLY place in the application where System. out is referenced directly.
     *
     * @return A new CliView instance for console I/O
     */
    @SuppressWarnings("java:S106") // System.out is intentional and necessary for CLI output
    public static CliView createStandardCliView() {
        PrintWriter writer = new PrintWriter(System.out, true);
        Scanner scanner = new Scanner(System.in);
        return new CliView(writer, scanner);
    }
}
