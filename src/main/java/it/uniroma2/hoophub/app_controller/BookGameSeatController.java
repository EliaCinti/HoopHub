package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.api.MockNbaScheduleApi;
import it.uniroma2.hoophub.api.dto.NbaApiDto;
import it.uniroma2.hoophub.beans.NbaGameBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application controller for the "Book Game Seat" use case.
 *
 * <p>Handles the booking flow for fans: fetching upcoming games,
 * finding venues that broadcast selected games, checking seat availability,
 * and creating booking requests with notifications.</p>
 *
 * <p>Used by both GUI and CLI graphic controllers.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class BookGameSeatController {

    private static final Logger LOGGER = Logger.getLogger(BookGameSeatController.class.getName());

    /** Number of days ahead to show games (2 weeks). */
    private static final int SCHEDULE_DAYS_AHEAD = 14;


    private final MockNbaScheduleApi nbaApi;
    private final VenueDao venueDao;
    private final BookingDao bookingDao;
    private final FanDao fanDao;

    /**
     * Default constructor using DaoFactoryFacade.
     */
    public BookGameSeatController() {
        this.nbaApi = new MockNbaScheduleApi();
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        this.venueDao = daoFactory.getVenueDao();
        this.bookingDao = daoFactory.getBookingDao();
        this.fanDao = daoFactory.getFanDao();
    }

    // ==================== STEP 1: GET UPCOMING GAMES ====================

    /**
     * Retrieves upcoming NBA games within the schedule window (2 weeks).
     *
     * @return List of NbaGameBean for display
     */
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

    // ==================== STEP 2: GET VENUES FOR GAME ====================

    /**
     * Retrieves all venues that broadcast at least one of the teams in the game.
     *
     * @param game The selected game
     * @return List of VenueBean with availability info
     * @throws DAOException if database access fails
     */
    public List<VenueBean> getVenuesForGame(NbaGameBean game) throws DAOException {
        List<Venue> allVenues = venueDao.retrieveAllVenues();

        return allVenues.stream()
                .filter(venue -> venueShowsGame(venue, game))
                .map(this::convertToVenueBean)
                .toList();
    }

    /**
     * Retrieves venues for a game with optional filters.
     * Optimized to query the DB by city if a city filter is provided.
     *
     * @param game              The selected game
     * @param cityFilter        Filter by city (null or empty to skip)
     * @param typeFilter        Filter by venue type (null to skip)
     * @param onlyWithSeats     If true, only return venues with available seats
     * @return Filtered list of VenueBean
     * @throws DAOException if database access fails
     */
    public List<VenueBean> getVenuesForGame(NbaGameBean game, String cityFilter,
                                            String typeFilter, boolean onlyWithSeats) throws DAOException {

        List<Venue> rawVenues;

        // 1. OTTIMIZZAZIONE: Se c'è un filtro città, usa il metodo specifico del DAO.
        // Altrimenti, scarica tutte le venue.
        if (cityFilter != null && !cityFilter.isEmpty()) {
            rawVenues = venueDao.retrieveVenuesByCity(cityFilter);
        } else {
            rawVenues = venueDao.retrieveAllVenues();
        }

        // 2. Pipeline di filtraggio e conversione
        return rawVenues.stream()
                // A. Filtro Business: La venue trasmette questa partita? (Squadra Casa o Ospite)
                .filter(venue -> venueShowsGame(venue, game))

                // B. Conversione: Da Model (Venue) a Bean (VenueBean)
                .map(this::convertToVenueBean)

                // C. Filtro Tipo (In memoria)
                .filter(v -> typeFilter == null || typeFilter.isEmpty()
                        || v.getType().name().equalsIgnoreCase(typeFilter))

                // D. Filtro Posti (Richiede query extra per i posti, quindi va fatto alla fine)
                .filter(v -> !onlyWithSeats || getAvailableSeats(v.getId(), game) > 0)

                .toList();
    }

    /**
     * Gets all unique cities from venues that show a specific game.
     *
     * @param game The selected game
     * @return List of city names for filter dropdown
     * @throws DAOException if database access fails
     */
    public List<String> getAvailableCitiesForGame(NbaGameBean game) throws DAOException {
        return getVenuesForGame(game).stream()
                .map(VenueBean::getCity)
                .distinct()
                .sorted()
                .toList();
    }

    // ==================== STEP 3: CHECK AVAILABILITY ====================

    /**
     * Calculates available seats for a venue and game.
     *
     * <p>Formula: maxCapacity - count(PENDING + CONFIRMED bookings for the same venue/game)</p>
     *
     * @param venueId The venue ID
     * @param game    The game
     * @return Number of available seats (0 if full)
     */
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

    /**
     * Checks if a venue has available seats for a game.
     *
     * @param venueId The venue ID
     * @param game    The game
     * @return true if at least one seat is available
     */
    public boolean hasAvailableSeats(int venueId, NbaGameBean game) {
        return getAvailableSeats(venueId, game) > 0;
    }

    /**
     * Checks if the current fan has already booked this game.
     *
     * @param game The game to check
     * @return true if already booked (PENDING or CONFIRMED)
     * @throws DAOException if database access fails
     * @throws UserSessionException if no fan is logged in
     */
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

    // ==================== STEP 4: CREATE BOOKING ====================

    /**
     * Creates a new booking request (status: PENDING).
     *
     * <p>Also creates a notification for the VenueManager.</p>
     *
     * @param game    The selected game
     * @param venueId The selected venue ID
     * @throws UserSessionException if no fan is logged in
     * @throws DAOException         if database access fails
     * @throws IllegalStateException if no seats available
     */
    public void createBookingRequest(NbaGameBean game, int venueId)
            throws UserSessionException, DAOException {

        // Verify fan is logged in
        UserBean currentUser = getCurrentFan();

        // Verify seats available
        if (!hasAvailableSeats(venueId, game)) {
            throw new IllegalStateException("No seats available for this game at this venue");
        }

        // Get entities
        Venue venue = venueDao.retrieveVenue(venueId);
        if (venue == null) {
            throw new DAOException("Venue not found: " + venueId);
        }

        Fan fan = fanDao.retrieveFan(currentUser.getUsername());
        if (fan == null) {
            throw new DAOException("Fan not found: " + currentUser.getUsername());
        }

        // Create booking
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

    // ==================== HELPER METHODS ====================

    /**
     * Gets the current logged-in user and verifies they are a fan.
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
     * Checks if a venue broadcasts at least one team from the game.
     */
    private boolean venueShowsGame(Venue venue, NbaGameBean game) {
        Set<TeamNBA> venueTeams = venue.getAssociatedTeams();
        return venueTeams.contains(game.getHomeTeam())
                || venueTeams.contains(game.getAwayTeam());
    }

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
}
