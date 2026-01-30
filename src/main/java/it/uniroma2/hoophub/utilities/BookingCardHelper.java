package it.uniroma2.hoophub.utilities;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.enums.BookingStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.time.format.DateTimeFormatter;

/**
 * Utility class for creating booking card UI components.
 *
 * <p>Provides reusable methods for building consistent booking card elements
 * across different views (ViewBookings, ManageSeats).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class BookingCardHelper {


    // Date/Time formatters
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Style classes
    private static final String STYLE_BADGE = "badge";
    private static final String STYLE_BADGE_WARNING = "badge-warning";
    private static final String STYLE_BADGE_SUCCESS = "badge-success";
    private static final String STYLE_BADGE_ERROR = "badge-error";
    private static final String STYLE_BADGE_MUTED = "badge-muted";
    private static final String STYLE_CARD_TITLE = "card-title";
    private static final String STYLE_CARD_DETAIL = "card-detail";

    private BookingCardHelper() {
        // Utility class - prevent instantiation
    }

    // ==================== STATUS BADGE ====================

    /**
     * Creates a status badge label with appropriate styling.
     *
     * @param status The booking status
     * @return Styled Label representing the status
     */
    public static Label createStatusBadge(BookingStatus status) {
        Label badge = new Label(status.name());
        badge.getStyleClass().add(STYLE_BADGE);
        badge.getStyleClass().add(getStatusBadgeStyle(status));
        return badge;
    }

    /**
     * Gets the CSS style class for a booking status.
     *
     * @param status The booking status
     * @return CSS style class name
     */
    public static String getStatusBadgeStyle(BookingStatus status) {
        return switch (status) {
            case PENDING -> STYLE_BADGE_WARNING;
            case CONFIRMED -> STYLE_BADGE_SUCCESS;
            case REJECTED -> STYLE_BADGE_ERROR;
            case CANCELLED -> STYLE_BADGE_MUTED;
        };
    }

    // ==================== HEADER ROW ====================

    /**
     * Creates the base header row with matchup label, spacer, and status badge.
     * <p>Delegates to {@link #createHeaderRowWithExtra(BookingBean, Label)} passing null.</p>
     *
     * @param booking The booking data
     * @return HBox containing matchup, spacer, and status badge
     */
    public static HBox createHeaderRowBase(BookingBean booking) {
        return createHeaderRowWithExtra(booking, null);
    }

    /**
     * Creates the header row with an optional label before the status badge.
     *
     * @param booking The booking data
     * @param extraLabel Additional label to insert before status badge (can be null)
     * @return HBox containing matchup, spacer, extra label (if present), and status badge
     */
    public static HBox createHeaderRowWithExtra(BookingBean booking, Label extraLabel) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Matchup label
        Label matchupLabel = new Label(formatMatchup(booking));
        matchupLabel.getStyleClass().add(STYLE_CARD_TITLE);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status badge
        Label statusBadge = createStatusBadge(booking.getStatus());

        if (extraLabel != null) {
            row.getChildren().addAll(matchupLabel, spacer, extraLabel, statusBadge);
        } else {
            row.getChildren().addAll(matchupLabel, spacer, statusBadge);
        }

        return row;
    }

    // ==================== DETAILS ROW ====================

    /**
     * Creates the details row with date, time, and venue.
     *
     * @param booking The booking data
     * @return HBox containing date, time, and venue labels
     */
    public static HBox createDetailsRow(BookingBean booking) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label(booking.getGameDate().format(DATE_FORMATTER));
        dateLabel.getStyleClass().add(STYLE_CARD_DETAIL);

        Label timeLabel = new Label(booking.getGameTime().format(TIME_FORMATTER));
        timeLabel.getStyleClass().add(STYLE_CARD_DETAIL);

        Label venueLabel = new Label("📍 " + booking.getVenueName());
        venueLabel.getStyleClass().add(STYLE_CARD_DETAIL);

        row.getChildren().addAll(dateLabel, timeLabel, venueLabel);
        return row;
    }

    // ==================== FORMATTING ====================

    /**
     * Formats the matchup string (Away @ Home).
     *
     * @param booking The booking data
     * @return Formatted matchup string
     */
    public static String formatMatchup(BookingBean booking) {
        return booking.getAwayTeam().getDisplayName() + " @ " +
                booking.getHomeTeam().getDisplayName();
    }

    // ==================== BADGE LABELS ====================

    /**
     * Creates a "can cancel" indicator badge.
     *
     * @return Styled label for cancellable bookings
     */
    public static Label createCancellableBadge() {
        Label label = new Label("(can cancel)");
        label.getStyleClass().addAll(STYLE_BADGE, STYLE_BADGE_SUCCESS);
        return label;
    }
}