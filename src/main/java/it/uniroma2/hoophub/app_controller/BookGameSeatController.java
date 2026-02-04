package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.api.MockNbaScheduleApi;
import it.uniroma2.hoophub.api.dto.NbaApiDto;
import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.beans.NbaGameBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.enums.NotificationType;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified application controller for the "Book Game Seat" use case.
 *
 * <p>Implements both {@link FanBooking} and {@link VenueManagerBooking} interfaces,
 * following the Interface Segregation Principle (ISP). Each actor (Fan, VenueManager)
 * only sees the methods relevant to their role.</p>
 *
 * <h3>Fan Operations (via FanBooking interface)</h3>
 * <ul>
 *   <li>Browse upcoming NBA games</li>
 *   <li>Find venues broadcasting a game</li>
 *   <li>Create booking requests</li>
 *   <li>View and cancel own bookings</li>
 * </ul>
 *
 * <h3>VenueManager Operations (via VenueManagerBooking interface)</h3>
 * <ul>
 *   <li>View booking requests for owned venues</li>
 *   <li>Approve or reject bookings</li>
 * </ul>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class BookGameSeatController implements FanBooking, VenueManagerBooking {

    private static final Logger LOGGER = Logger.getLogger(BookGameSeatController.class.getName());

    /** Number of days ahead to show games (2 weeks). */
    private static final int SCHEDULE_DAYS_AHEAD = 14;

    /** Days before game date when cancellation is no longer allowed. */
    private static final int CANCELLATION_DEADLINE_DAYS = 2;

    // DAOs
    private final MockNbaScheduleApi nbaApi;
    private final VenueDao venueDao;
    private final BookingDao bookingDao;
    private final FanDao fanDao;
    private final NotificationDao notificationDao;

    /**
     * Default constructor using DaoFactoryFacade.
     */
    public BookGameSeatController() {
        this.nbaApi = new MockNbaScheduleApi();
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        this.venueDao = daoFactory.getVenueDao();
        this.bookingDao = daoFactory.getBookingDao();
        this.fanDao = daoFactory.getFanDao();
        this.notificationDao = daoFactory.getNotificationDao();
    }

    // ========================================================================
    // FAN BOOKING INTERFACE IMPLEMENTATION
    // ========================================================================

    // ==================== GAME BROWSING ====================

    @Override
    public List<NbaGameBean> getUpcomingGames() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(SCHEDULE_DAYS_AHEAD);

        List<NbaApiDto> rawGames = nbaApi.fetchRawGames();

        return rawGames.stream()
                .map(this::convertToGameBean)
                .filter(Objects::nonNull)
                .filter(game -> !game.getDate().isBefore(today) && !game.getDate().isAfter(endDate))
                .sorted(Comparator.comparing(NbaGameBean::getDate)
                        .thenComparing(NbaGameBean::getTime))
                .toList();
    }

    // ==================== VENUE SELECTION ====================

    @Override
    public List<VenueBean> getVenuesForGame(NbaGameBean game) throws DAOException {
        List<Venue> allVenues = venueDao.retrieveAllVenues();

        return allVenues.stream()
                .filter(venue -> venueShowsGame(venue, game))
                .map(this::convertToVenueBean)
                .toList();
    }

    @Override
    public List<VenueBean> getVenuesForGame(NbaGameBean game, String cityFilter,
                                            String typeFilter, boolean onlyWithSeats) throws DAOException {

        List<Venue> rawVenues;

        if (cityFilter != null && !cityFilter.isEmpty()) {
            rawVenues = venueDao.retrieveVenuesByCity(cityFilter);
        } else {
            rawVenues = venueDao.retrieveAllVenues();
        }

        return rawVenues.stream()
                .filter(venue -> venueShowsGame(venue, game))
                .map(this::convertToVenueBean)
                .filter(v -> typeFilter == null || typeFilter.isEmpty()
                        || v.getType().name().equalsIgnoreCase(typeFilter))
                .filter(v -> !onlyWithSeats || getAvailableSeats(v.getId(), game) > 0)
                .toList();
    }

    @Override
    public List<String> getAvailableCitiesForGame(NbaGameBean game) throws DAOException {
        return getVenuesForGame(game).stream()
                .map(VenueBean::getCity)
                .distinct()
                .sorted()
                .toList();
    }

    // ==================== SEAT AVAILABILITY ====================

    @Override
    public int getAvailableSeats(int venueId, NbaGameBean game) {
        try {
            Venue venue = venueDao.retrieveVenue(venueId);
            if (venue == null) {
                return 0;
            }

            List<Booking> bookings = bookingDao.retrieveBookingsByVenue(venueId);

            long occupiedSeats = bookings.stream()
                    .filter(b -> b.getGameDate().equals(game.getDate()))
                    .filter(b -> b.getHomeTeam() == game.getHomeTeam())
                    .filter(b -> b.getAwayTeam() == game.getAwayTeam())
                    .filter(b -> b.getStatus() == BookingStatus.PENDING
                            || b.getStatus() == BookingStatus.CONFIRMED)
                    .count();

            return Math.max(0, venue.getMaxCapacity() - (int) occupiedSeats);
        } catch (DAOException e) {
            LOGGER.log(Level.WARNING, "Error calculating available seats", e);
            return 0;
        }
    }

    @Override
    public boolean hasAvailableSeats(int venueId, NbaGameBean game) {
        return getAvailableSeats(venueId, game) > 0;
    }

    @Override
    public boolean hasAlreadyBooked(NbaGameBean game) throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentFan();
        List<Booking> fanBookings = bookingDao.retrieveBookingsByFan(currentUser.getUsername());

        return fanBookings.stream()
                .anyMatch(b -> b.getGameDate().equals(game.getDate())
                        && b.getHomeTeam() == game.getHomeTeam()
                        && b.getAwayTeam() == game.getAwayTeam()
                        && (b.getStatus() == BookingStatus.PENDING
                        || b.getStatus() == BookingStatus.CONFIRMED));
    }

    // ==================== BOOKING CREATION ====================

    @Override
    public void createBookingRequest(NbaGameBean game, int venueId)
            throws UserSessionException, DAOException {

        UserBean currentUser = getCurrentFan();

        if (!hasAvailableSeats(venueId, game)) {
            throw new IllegalStateException("No seats available for this game at this venue");
        }

        Venue venue = venueDao.retrieveVenue(venueId);
        if (venue == null) {
            throw new DAOException("Venue not found: " + venueId);
        }

        Fan fan = fanDao.retrieveFan(currentUser.getUsername());
        if (fan == null) {
            throw new DAOException("Fan not found: " + currentUser.getUsername());
        }

        int bookingId = bookingDao.getNextBookingId();
        Booking booking = new Booking.Builder(
                bookingId,
                game.getDate(),
                game.getTime(),
                game.getHomeTeam(),
                game.getAwayTeam(),
                venue,
                fan
        ).status(BookingStatus.PENDING).build();

        bookingDao.saveBooking(booking);

        LOGGER.log(Level.INFO, "Booking request created: {0}", booking);
    }

    // ==================== FAN BOOKING MANAGEMENT ====================

    @Override
    public List<BookingBean> getMyBookings() throws DAOException, UserSessionException {
        return getMyBookings(null);
    }

    @Override
    public List<BookingBean> getMyBookings(BookingStatus statusFilter)
            throws DAOException, UserSessionException {

        UserBean currentUser = getCurrentFan();
        String username = currentUser.getUsername();

        List<Booking> bookings;
        if (statusFilter != null) {
            bookings = bookingDao.retrieveBookingsByStatus(username, statusFilter);
        } else {
            bookings = bookingDao.retrieveBookingsByFan(username);
        }

        List<BookingBean> bookingBeans = new ArrayList<>();
        for (Booking booking : bookings) {
            Venue venue = venueDao.retrieveVenue(booking.getVenueId());
            bookingBeans.add(convertToBookingBean(booking, venue));
        }

        bookingBeans.sort(Comparator
                .comparing((BookingBean b) -> isTerminalStatus(b.getStatus()))
                .thenComparing(BookingBean::getGameDate, Comparator.reverseOrder()));

        return bookingBeans;
    }

    @Override
    public List<BookingBean> getCancellableBookings() throws DAOException, UserSessionException {
        List<BookingBean> allBookings = getMyBookings();
        return allBookings.stream()
                .filter(this::canBeCancelled)
                .toList();
    }

    @Override
    public void cancelBooking(int bookingId) throws DAOException, UserSessionException {
        Booking booking = getAndValidateFanBooking(bookingId);

        validateCancellation(booking);

        booking.cancel();
        bookingDao.updateBooking(booking);

        notifyVenueManagerOfCancellation(booking);

        LOGGER.log(Level.INFO, "Booking {0} cancelled by fan", bookingId);
    }

    @Override
    public boolean canCancelBooking(int bookingId) throws DAOException, UserSessionException {
        try {
            Booking booking = getAndValidateFanBooking(bookingId);
            validateCancellation(booking);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    // ==================== FAN NOTIFICATIONS ====================

    @Override
    public int getFanUnreadNotificationsCount() throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentFan();
        return notificationDao.getUnreadCount(currentUser.getUsername(), UserType.FAN);
    }

    @Override
    public void markFanNotificationsAsRead() throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentFan();
        DaoFactoryFacade.getInstance().markAllNotificationsAsRead(
                currentUser.getUsername(), UserType.FAN);

        LOGGER.log(Level.INFO, "Marked all notifications as read for Fan: {0}",
                currentUser.getUsername());
    }

    // ========================================================================
    // VENUE MANAGER BOOKING INTERFACE IMPLEMENTATION
    // ========================================================================

    // ==================== VM BOOKING RETRIEVAL ====================

    @Override
    public List<BookingBean> getBookingsForMyVenues() throws DAOException, UserSessionException {
        return getBookingsForMyVenues(null);
    }

    @Override
    public List<BookingBean> getBookingsForMyVenues(BookingStatus statusFilter)
            throws DAOException, UserSessionException {

        UserBean currentUser = getCurrentVenueManager();

        List<Booking> allBookings = bookingDao.retrieveBookingsByVenueManager(currentUser.getUsername());
        List<BookingBean> result = new ArrayList<>();

        for (Booking booking : allBookings) {
            if (statusFilter == null || booking.getStatus() == statusFilter) {
                Venue venue = venueDao.retrieveVenue(booking.getVenueId());
                BookingBean bean = convertToBookingBean(booking, venue);
                result.add(bean);
            }
        }

        result.sort((b1, b2) -> {
            if (b1.getStatus() == BookingStatus.PENDING && b2.getStatus() != BookingStatus.PENDING) {
                return -1;
            }
            if (b2.getStatus() == BookingStatus.PENDING && b1.getStatus() != BookingStatus.PENDING) {
                return 1;
            }
            return b2.getGameDate().compareTo(b1.getGameDate());
        });

        return result;
    }

    @Override
    public List<BookingBean> getPendingBookings() throws DAOException, UserSessionException {
        return getBookingsForMyVenues(BookingStatus.PENDING);
    }

    // ==================== VM BOOKING ACTIONS ====================

    @Override
    public void approveBooking(int bookingId) throws DAOException, UserSessionException {
        Booking booking = getAndValidateVmBooking(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be approved");
        }

        booking.confirm();
        bookingDao.updateBooking(booking);

        LOGGER.log(Level.INFO, "Booking {0} approved", bookingId);
    }

    @Override
    public void rejectBooking(int bookingId) throws DAOException, UserSessionException {
        Booking booking = getAndValidateVmBooking(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be rejected");
        }

        booking.reject();
        bookingDao.updateBooking(booking);

        LOGGER.log(Level.INFO, "Booking {0} rejected", bookingId);
    }

    // ==================== VM NOTIFICATIONS ====================

    @Override
    public int getVmUnreadNotificationsCount() throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentVenueManager();
        return notificationDao.getUnreadCount(currentUser.getUsername(), UserType.VENUE_MANAGER);
    }

    @Override
    public void markVmNotificationsAsRead() throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentVenueManager();
        DaoFactoryFacade.getInstance().markAllNotificationsAsRead(
                currentUser.getUsername(), UserType.VENUE_MANAGER);

        LOGGER.log(Level.INFO, "Marked all notifications as read for VenueManager: {0}",
                currentUser.getUsername());
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    // ==================== SESSION HELPERS ====================

    /**
     * Gets the current logged-in Fan.
     */
    private UserBean getCurrentFan() throws UserSessionException {
        UserBean currentUser = SessionManager.INSTANCE.getCurrentUser();

        if (currentUser == null) {
            throw new UserSessionException("No user logged in");
        }

        if (currentUser.getType() != UserType.FAN) {
            throw new UserSessionException("Current user is not a fan");
        }

        return currentUser;
    }

    /**
     * Gets the current logged-in VenueManager.
     */
    private UserBean getCurrentVenueManager() throws UserSessionException {
        UserBean currentUser = SessionManager.INSTANCE.getCurrentUser();

        if (currentUser == null) {
            throw new UserSessionException("No user logged in");
        }

        if (currentUser.getType() != UserType.VENUE_MANAGER) {
            throw new UserSessionException("Current user is not a VenueManager");
        }

        return currentUser;
    }

    // ==================== VALIDATION HELPERS ====================

    /**
     * Retrieves and validates Fan booking ownership.
     */
    private Booking getAndValidateFanBooking(int bookingId) throws DAOException, UserSessionException {
        Booking booking = bookingDao.retrieveBooking(bookingId);
        if (booking == null) {
            throw new IllegalStateException("Booking not found: " + bookingId);
        }

        UserBean currentUser = getCurrentFan();
        if (!booking.getFanUsername().equals(currentUser.getUsername())) {
            throw new IllegalStateException("Booking does not belong to you");
        }

        return booking;
    }

    /**
     * Retrieves and validates VenueManager booking ownership.
     */
    private Booking getAndValidateVmBooking(int bookingId) throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentVenueManager();

        Booking booking = bookingDao.retrieveBooking(bookingId);
        if (booking == null) {
            throw new IllegalStateException("Booking not found: " + bookingId);
        }

        Venue venue = venueDao.retrieveVenue(booking.getVenueId());
        if (venue == null || !venue.getVenueManagerUsername().equals(currentUser.getUsername())) {
            throw new IllegalStateException("Booking does not belong to your venues");
        }

        return booking;
    }

    /**
     * Validates that a booking can be canceled.
     */
    private void validateCancellation(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only PENDING or CONFIRMED bookings can be cancelled");
        }

        LocalDate deadline = booking.getGameDate().minusDays(CANCELLATION_DEADLINE_DAYS);
        if (LocalDate.now().isAfter(deadline)) {
            throw new IllegalStateException(
                    String.format("Cannot cancel bookings within %d days of the game", CANCELLATION_DEADLINE_DAYS));
        }
    }

    // ==================== STATUS HELPERS ====================

    /**
     * Checks if a status is terminal (cannot be canceled).
     */
    private boolean isTerminalStatus(BookingStatus status) {
        return status == BookingStatus.REJECTED || status == BookingStatus.CANCELLED;
    }

    /**
     * Checks if a booking can be canceled (status + deadline).
     */
    private boolean canBeCancelled(BookingBean booking) {
        if (isTerminalStatus(booking.getStatus())) {
            return false;
        }
        LocalDate deadline = booking.getGameDate().minusDays(CANCELLATION_DEADLINE_DAYS);
        return !LocalDate.now().isAfter(deadline);
    }

    // ==================== VENUE HELPERS ====================

    /**
     * Checks if a venue broadcasts at least one team from the game.
     */
    private boolean venueShowsGame(Venue venue, NbaGameBean game) {
        Set<TeamNBA> venueTeams = venue.getAssociatedTeams();
        return venueTeams.contains(game.getHomeTeam())
                || venueTeams.contains(game.getAwayTeam());
    }

    // ==================== NOTIFICATION HELPERS ====================

    /**
     * Notifies the VenueManager about a cancellation.
     */
    private void notifyVenueManagerOfCancellation(Booking booking) throws DAOException {
        Venue venue = venueDao.retrieveVenue(booking.getVenueId());
        if (venue == null) {
            LOGGER.log(Level.WARNING, "Venue not found for booking {0}", booking.getId());
            return;
        }

        Notification notification = new Notification.Builder()
                .username(venue.getVenueManagerUsername())
                .userType(UserType.VENUE_MANAGER)
                .type(NotificationType.BOOKING_CANCELLED)
                .message(String.format("Booking cancelled: %s for %s on %s",
                        booking.getFanUsername(),
                        booking.getMatchup(),
                        booking.getGameDate()))
                .bookingId(booking.getId())
                .build();

        notificationDao.saveNotification(notification);
    }

    // ==================== CONVERSION HELPERS ====================

    /**
     * Converts NbaApiDto to NbaGameBean.
     */
    private NbaGameBean convertToGameBean(NbaApiDto dto) {
        try {
            TeamNBA homeTeam = TeamNBA.fromAbbreviation(dto.homeTeamCode());
            TeamNBA awayTeam = TeamNBA.fromAbbreviation(dto.awayTeamCode());

            if (homeTeam == null || awayTeam == null) {
                LOGGER.log(Level.WARNING, "Unknown team code: {0} or {1}",
                        new Object[]{dto.homeTeamCode(), dto.awayTeamCode()});
                return null;
            }

            return new NbaGameBean.Builder()
                    .gameId(dto.id())
                    .homeTeam(homeTeam)
                    .awayTeam(awayTeam)
                    .date(LocalDate.parse(dto.date()))
                    .time(LocalTime.parse(dto.time()))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error converting game DTO", e);
            return null;
        }
    }

    /**
     * Converts Venue model to VenueBean.
     */
    private VenueBean convertToVenueBean(Venue venue) {
        return new VenueBean.Builder()
                .id(venue.getId())
                .name(venue.getName())
                .type(venue.getType())
                .address(venue.getAddress())
                .city(venue.getCity())
                .maxCapacity(venue.getMaxCapacity())
                .venueManagerUsername(venue.getVenueManagerUsername())
                .associatedTeams(venue.getAssociatedTeams())
                .build();
    }

    /**
     * Converts a Booking model to BookingBean.
     */
    private BookingBean convertToBookingBean(Booking booking, Venue venue) {
        return new BookingBean.Builder()
                .id(booking.getId())
                .gameDate(booking.getGameDate())
                .gameTime(booking.getGameTime())
                .homeTeam(booking.getHomeTeam())
                .awayTeam(booking.getAwayTeam())
                .venueId(booking.getVenueId())
                .venueName(venue != null ? venue.getName() : "Unknown")
                .fanUsername(booking.getFanUsername())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}