package it.uniroma2.hoophub.dao.inmemory;

import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.model.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized in-memory data store implementing <b>Singleton pattern (GoF)</b>.
 *
 * <p>Acts as a "RAM database" for the IN_MEMORY persistence type.
 * All InMemory DAOs read/write to this shared store.</p>
 *
 * <h3>Design Rationale</h3>
 * <ul>
 *   <li>Demonstrates <b>Dependency Inversion Principle</b>: business logic
 *       works identically regardless of persistence mechanism</li>
 *   <li>Provides demo/testing mode without external dependencies</li>
 *   <li>Uses {@link ConcurrentHashMap} for thread safety</li>
 *   <li>Uses {@link AtomicInteger} for auto-increment IDs</li>
 * </ul>
 *
 * <h3>Data Lifecycle</h3>
 * <p>Data exists only during application runtime. On application exit,
 * all data is lost. This is intentional for demo purposes.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
@SuppressWarnings("java:S6548")
public class InMemoryDataStore {

    private static final Logger LOGGER = Logger.getLogger(InMemoryDataStore.class.getName());

    private static final InMemoryDataStore INSTANCE = new InMemoryDataStore();

    // User data (mirrors MySQL 'users' table - stores Fan/VenueManager via polymorphism)
    private final Map<String, User> users;

    // Fan-specific data (mirrors MySQL 'fans' table)
    private final Map<String, Fan> fans;

    // VenueManager-specific data (mirrors MySQL 'venue_managers' table)
    private final Map<String, VenueManager> venueManagers;

    // Venue data
    private final Map<Integer, Venue> venues;

    // Booking data
    private final Map<Integer, Booking> bookings;

    // Notification data
    private final Map<Integer, Notification> notifications;

    // Venue-Team associations (venueId -> Set of teams)
    private final Map<Integer, Set<TeamNBA>> venueTeams;

    // Auto-increment ID generators
    private final AtomicInteger venueIdGenerator;
    private final AtomicInteger bookingIdGenerator;
    private final AtomicInteger notificationIdGenerator;

    private InMemoryDataStore() {
        this.users = new ConcurrentHashMap<>();
        this.fans = new ConcurrentHashMap<>();
        this.venueManagers = new ConcurrentHashMap<>();
        this.venues = new ConcurrentHashMap<>();
        this.bookings = new ConcurrentHashMap<>();
        this.notifications = new ConcurrentHashMap<>();
        this.venueTeams = new ConcurrentHashMap<>();

        this.venueIdGenerator = new AtomicInteger(1);
        this.bookingIdGenerator = new AtomicInteger(1);
        this.notificationIdGenerator = new AtomicInteger(1);

        LOGGER.info("InMemoryDataStore initialized");
    }

    /**
     * Returns the singleton instance.
     *
     * @return the InMemoryDataStore instance
     */
    public static InMemoryDataStore getInstance() {
        return INSTANCE;
    }

    // ========================================================================
    // USER OPERATIONS (base user data - stores Fan/VenueManager polymorphic)
    // ========================================================================

    public void saveUser(User user) {
        users.put(user.getUsername(), user);
        LOGGER.log(Level.FINE, "User saved: {0}", user.getUsername());
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    public void deleteUser(String username) {
        users.remove(username);
        LOGGER.log(Level.FINE, "User deleted: {0}", username);
    }

    // ========================================================================
    // FAN OPERATIONS
    // ========================================================================

    public void saveFan(Fan fan) {
        fans.put(fan.getUsername(), fan);
        LOGGER.log(Level.FINE, "Fan saved: {0}", fan.getUsername());
    }

    public Fan getFan(String username) {
        return fans.get(username);
    }

    public Map<String, Fan> getAllFans() {
        return new ConcurrentHashMap<>(fans);
    }

    public void deleteFan(String username) {
        fans.remove(username);
        LOGGER.log(Level.FINE, "Fan deleted: {0}", username);
    }

    // ========================================================================
    // VENUE MANAGER OPERATIONS
    // ========================================================================

    public void saveVenueManager(VenueManager vm) {
        venueManagers.put(vm.getUsername(), vm);
        LOGGER.log(Level.FINE, "VenueManager saved: {0}", vm.getUsername());
    }

    public VenueManager getVenueManager(String username) {
        return venueManagers.get(username);
    }

    public Map<String, VenueManager> getAllVenueManagers() {
        return new ConcurrentHashMap<>(venueManagers);
    }

    public void deleteVenueManager(String username) {
        venueManagers.remove(username);
        LOGGER.log(Level.FINE, "VenueManager deleted: {0}", username);
    }

    // ========================================================================
    // VENUE OPERATIONS
    // ========================================================================

    public int getNextVenueId() {
        return venueIdGenerator.getAndIncrement();
    }

    public void saveVenue(Venue venue) {
        venues.put(venue.getId(), venue);
        LOGGER.log(Level.FINE, "Venue saved: {0}", venue.getId());
    }

    public Venue getVenue(int venueId) {
        return venues.get(venueId);
    }

    public Map<Integer, Venue> getAllVenues() {
        return new ConcurrentHashMap<>(venues);
    }

    public boolean venueExists(int venueId) {
        return venues.containsKey(venueId);
    }

    public void deleteVenue(int venueId) {
        venues.remove(venueId);
        venueTeams.remove(venueId);
        LOGGER.log(Level.FINE, "Venue deleted: {0}", venueId);
    }

    // ========================================================================
    // VENUE-TEAM ASSOCIATIONS
    // ========================================================================

    public void saveVenueTeam(int venueId, TeamNBA team) {
        venueTeams.computeIfAbsent(venueId, k -> ConcurrentHashMap.newKeySet()).add(team);
        LOGGER.log(Level.FINE, "Team {0} associated with Venue {1}", new Object[]{team, venueId});
    }

    public void deleteVenueTeam(int venueId, TeamNBA team) {
        Set<TeamNBA> teams = venueTeams.get(venueId);
        if (teams != null) {
            teams.remove(team);
        }
    }

    public Set<TeamNBA> getVenueTeams(int venueId) {
        return venueTeams.getOrDefault(venueId, ConcurrentHashMap.newKeySet());
    }

    public void deleteAllVenueTeams(int venueId) {
        venueTeams.remove(venueId);
    }

    // ========================================================================
    // BOOKING OPERATIONS
    // ========================================================================

    public int getNextBookingId() {
        return bookingIdGenerator.getAndIncrement();
    }

    public void saveBooking(Booking booking) {
        bookings.put(booking.getId(), booking);
        LOGGER.log(Level.FINE, "Booking saved: {0}", booking.getId());
    }

    public Booking getBooking(int bookingId) {
        return bookings.get(bookingId);
    }

    public Map<Integer, Booking> getAllBookings() {
        return new ConcurrentHashMap<>(bookings);
    }

    public boolean bookingExists(int bookingId) {
        return bookings.containsKey(bookingId);
    }

    public void deleteBooking(int bookingId) {
        bookings.remove(bookingId);
        LOGGER.log(Level.FINE, "Booking deleted: {0}", bookingId);
    }

    // ========================================================================
    // NOTIFICATION OPERATIONS
    // ========================================================================

    public int getNextNotificationId() {
        return notificationIdGenerator.getAndIncrement();
    }

    public void saveNotification(Notification notification) {
        notifications.put(notification.getId(), notification);
        LOGGER.log(Level.FINE, "Notification saved: {0}", notification.getId());
    }

    public Notification getNotification(int id) {
        return notifications.get(id);
    }

    public Map<Integer, Notification> getAllNotifications() {
        return new ConcurrentHashMap<>(notifications);
    }

    public void deleteNotification(int id) {
        notifications.remove(id);
        LOGGER.log(Level.FINE, "Notification deleted: {0}", id);
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Clears all data from the store.
     * Useful for testing or reset functionality.
     */
    public void clearAll() {
        users.clear();
        fans.clear();
        venueManagers.clear();
        venues.clear();
        bookings.clear();
        notifications.clear();
        venueTeams.clear();

        venueIdGenerator.set(1);
        bookingIdGenerator.set(1);
        notificationIdGenerator.set(1);

        LOGGER.info("InMemoryDataStore cleared completely");
    }

    /**
     * Returns statistics about stored data (for debugging).
     *
     * @return formatted string with counts
     */
    public String getStats() {
        return String.format(
                "InMemoryDataStore Stats: Users=%d, Fans=%d, VMs=%d, Venues=%d, Bookings=%d, Notifications=%d",
                users.size(), fans.size(), venueManagers.size(),
                venues.size(), bookings.size(), notifications.size()
        );
    }
}