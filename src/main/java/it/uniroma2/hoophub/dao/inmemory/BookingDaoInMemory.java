package it.uniroma2.hoophub.dao.inmemory;

import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.GlobalCache;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory implementation of {@link BookingDao}.
 *
 * <p>Stores Booking data in RAM via {@link InMemoryDataStore}.
 * Supports all query operations required by the booking workflow.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class BookingDaoInMemory extends AbstractObservableDao implements BookingDao {

    private static final Logger LOGGER = Logger.getLogger(BookingDaoInMemory.class.getName());

    private static final String BOOKING = "Booking";
    private static final String ERR_NULL_BOOKING = "Booking cannot be null";
    private static final String ERR_INVALID_ID = "Booking ID must be positive";
    private static final String ERR_BOOKING_NOT_FOUND = "Booking not found";

    private final InMemoryDataStore dataStore;
    private final GlobalCache cache;

    public BookingDaoInMemory() {
        this.dataStore = InMemoryDataStore.getInstance();
        this.cache = GlobalCache.getInstance();
    }

    @Override
    public void saveBooking(Booking booking) {
        validateBookingInput(booking);

        int bookingId = booking.getId();

        // Generate ID if not set
        if (bookingId <= 0) {
            bookingId = dataStore.getNextBookingId();
            booking = rebuildBookingWithId(booking, bookingId);
        }

        dataStore.saveBooking(booking);
        cache.put(generateCacheKey(bookingId), booking);

        LOGGER.log(Level.INFO, "Booking saved with ID: {0}", bookingId);
        notifyObservers(DaoOperation.INSERT, BOOKING, String.valueOf(bookingId), booking);
    }

    @Override
    public Booking retrieveBooking(int bookingId) {
        validateIdInput(bookingId);

        // Check cache first
        Booking cached = (Booking) cache.get(generateCacheKey(bookingId));
        if (cached != null) {
            return cached;
        }

        Booking booking = dataStore.getBooking(bookingId);
        if (booking != null) {
            cache.put(generateCacheKey(bookingId), booking);
        }

        return booking;
    }

    @Override
    public List<Booking> retrieveAllBookings() {
        return new ArrayList<>(dataStore.getAllBookings().values());
    }

    @Override
    public List<Booking> retrieveBookingsByFan(String fanUsername) {
        validateUsernameInput(fanUsername);

        List<Booking> result = new ArrayList<>();

        for (Booking booking : dataStore.getAllBookings().values()) {
            if (booking.getFan().getUsername().equals(fanUsername)) {
                result.add(booking);
            }
        }

        // Sort by date descending (newest first)
        result.sort(Comparator.comparing(Booking::getGameDate).reversed());
        return result;
    }

    @Override
    public List<Booking> retrieveBookingsByVenue(int venueId) {
        validateIdInput(venueId);

        List<Booking> result = new ArrayList<>();

        for (Booking booking : dataStore.getAllBookings().values()) {
            if (booking.getVenue().getId() == venueId) {
                result.add(booking);
            }
        }

        return result;
    }

    @Override
    public List<Booking> retrieveBookingsByVenueManager(String venueManagerUsername) {
        validateUsernameInput(venueManagerUsername);

        List<Booking> result = new ArrayList<>();

        for (Booking booking : dataStore.getAllBookings().values()) {
            if (booking.getVenue().getVenueManager().getUsername().equals(venueManagerUsername)) {
                result.add(booking);
            }
        }

        // Sort: PENDING first, then by date descending
        result.sort(Comparator
                .comparing((Booking b) -> b.getStatus() != BookingStatus.PENDING)
                .thenComparing(Booking::getGameDate, Comparator.reverseOrder()));

        return result;
    }

    @Override
    public List<Booking> retrieveBookingsByDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        List<Booking> result = new ArrayList<>();

        for (Booking booking : dataStore.getAllBookings().values()) {
            if (booking.getGameDate().equals(date)) {
                result.add(booking);
            }
        }

        return result;
    }

    @Override
    public List<Booking> retrieveBookingsByStatus(String fanUsername, BookingStatus status) {
        validateUsernameInput(fanUsername);
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        List<Booking> result = new ArrayList<>();

        for (Booking booking : dataStore.getAllBookings().values()) {
            if (booking.getFan().getUsername().equals(fanUsername) && booking.getStatus() == status) {
                result.add(booking);
            }
        }

        result.sort(Comparator.comparing(Booking::getGameDate).reversed());
        return result;
    }

    @Override
    public List<Booking> retrieveUnnotifiedBookings(String fanUsername) {
        validateUsernameInput(fanUsername);

        List<Booking> result = new ArrayList<>();

        for (Booking booking : dataStore.getAllBookings().values()) {
            if (booking.getFan().getUsername().equals(fanUsername) && !booking.isNotified()) {
                result.add(booking);
            }
        }

        return result;
    }

    @Override
    public void updateBooking(Booking booking) throws DAOException {
        validateBookingInput(booking);
        int bookingId = booking.getId();

        if (!dataStore.bookingExists(bookingId)) {
            throw new DAOException(ERR_BOOKING_NOT_FOUND + ": " + bookingId);
        }

        dataStore.saveBooking(booking);
        cache.put(generateCacheKey(bookingId), booking);

        LOGGER.log(Level.INFO, "Booking updated: {0}", bookingId);
        notifyObservers(DaoOperation.UPDATE, BOOKING, String.valueOf(bookingId), booking);
    }

    @Override
    public void deleteBooking(Booking booking) throws DAOException {
        validateBookingInput(booking);
        int bookingId = booking.getId();

        if (!dataStore.bookingExists(bookingId)) {
            throw new DAOException(ERR_BOOKING_NOT_FOUND + ": " + bookingId);
        }

        dataStore.deleteBooking(bookingId);
        cache.remove(generateCacheKey(bookingId));

        LOGGER.log(Level.INFO, "Booking deleted: {0}", bookingId);
        notifyObservers(DaoOperation.DELETE, BOOKING, String.valueOf(bookingId), null);
    }

    @Override
    public boolean bookingExists(int bookingId) {
        return dataStore.bookingExists(bookingId);
    }

    @Override
    public int getNextBookingId() {
        return dataStore.getNextBookingId();
    }

    // ========== PRIVATE HELPERS ==========

    private String generateCacheKey(int bookingId) {
        return "Booking:" + bookingId;
    }

    private void validateBookingInput(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException(ERR_NULL_BOOKING);
        }
    }

    private void validateIdInput(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(ERR_INVALID_ID);
        }
    }

    private void validateUsernameInput(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
    }

    /**
     * Rebuilds a Booking with a new ID (Bookings are immutable).
     */
    private Booking rebuildBookingWithId(Booking original, int newId) {
        return new Booking.Builder(
                newId,
                original.getGameDate(),
                original.getGameTime(),
                original.getHomeTeam(),
                original.getAwayTeam(),
                original.getVenue(),
                original.getFan()
        )
                .status(original.getStatus())
                .createdAt(original.getCreatedAt())
                .notified(original.isNotified())
                .build();
    }
}