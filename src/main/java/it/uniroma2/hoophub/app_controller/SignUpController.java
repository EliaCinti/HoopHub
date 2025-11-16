package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.model.UserType;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;

/**
 * SignUpController manages the registration process for new users.
 * <p>
 * This controller handles both Fan and VenueManager registration by
 * delegating to the appropriate DAOs and managing user sessions.
 * </p>
 * <p>
 * <strong>Singleton Pattern:</strong> This controller is a singleton because it manages application-level
 * use case logic without any UI-specific or session-specific state.
 * </p>
 */
@SuppressWarnings("java:S6548") // Singleton is required
public class SignUpController extends AbstractController {

    private static SignUpController instance;

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private SignUpController() {
        // Private constructor
    }

    /**
     * Returns the singleton instance of SignUpController.
     *
     * @return The singleton SignUpController instance
     */
    public static synchronized SignUpController getInstance() {
        if (instance == null) {
            instance = new SignUpController();
        }
        return instance;
    }

    /**
     * Registers a new Fan in the system.
     * <p>
     * Creates a new Fan account, stores it in persistence, and optionally
     * logs the user in automatically after successful registration.
     * </p>
     *
     * @param fanBean The fan registration data
     * @param autoLogin If true, automatically logs in the user after registration
     * @return A UserBean containing the registered user's data
     * @throws DAOException If there is an error saving the fan data
     * @throws UserSessionException If autoLogin is true and session creation fails
     */
    private UserBean signUpFan(FanBean fanBean, boolean autoLogin) throws DAOException, UserSessionException {
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        FanDao fanDao = daoFactory.getFanDao();

        // Save the fan to persistence
        fanDao.saveFan(fanBean);

        // Retrieve the complete Fan object from persistence
        Fan registeredFan = fanDao.retrieveFan(fanBean.getUsername());

        if (registeredFan == null) {
            throw new DAOException("Fan was saved but could not be retrieved. Database inconsistency!");
        }

        // Optionally log in the user automatically
        if (autoLogin) {
            storeUserSession(registeredFan);
        }

        return convertUserToBean(registeredFan);
    }

    /**
     * Registers a new VenueManager in the system.
     * <p>
     * Creates a new VenueManager account, stores it in persistence, and optionally
     * logs the user in automatically after successful registration.
     * </p>
     *
     * @param venueManagerBean The venue manager registration data
     * @param autoLogin If true, automatically logs in the user after registration
     * @return A UserBean containing the registered user's data
     * @throws DAOException If there is an error saving the venue manager data
     * @throws UserSessionException If autoLogin is true and session creation fails
     */
    private UserBean signUpVenueManager(VenueManagerBean venueManagerBean, boolean autoLogin)
            throws DAOException, UserSessionException {
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        VenueManagerDao venueManagerDao = daoFactory.getVenueManagerDao();

        // Save the venue manager to persistence
        venueManagerDao.saveVenueManager(venueManagerBean);

        // Retrieve the complete VenueManager object from persistence
        VenueManager registeredManager = venueManagerDao.retrieveVenueManager(venueManagerBean.getUsername());

        if (registeredManager == null) {
            throw new DAOException("VenueManager was saved but could not be retrieved. Database inconsistency!");
        }

        // Optionally log in the user automatically
        if (autoLogin) {
            storeUserSession(registeredManager);
        }

        return convertUserToBean(registeredManager);
    }

    /**
     * Generic signup method that routes to the appropriate registration method based on user type.
     * <p>
     * <strong>Polymorphism:</strong> This method uses pattern matching in switch expressions (Java 21+)
     * to dispatch to the appropriate registration method based on the runtime type of the user bean.
     * This is polymorphic behavior (runtime type checking and dispatch) that is type-safe and
     * benefits from exhaustiveness checking.
     * </p>
     *
     * @param userBean The user bean (must be FanBean or VenueManagerBean)
     * @param autoLogin If true, automatically logs in the user after registration
     * @return A UserBean containing the registered user's data
     * @throws DAOException If there is an error during registration
     * @throws UserSessionException If autoLogin is true and session creation fails
     * @throws IllegalArgumentException If the userBean type is not supported
     */
    public UserBean signUp(UserBean userBean, boolean autoLogin) throws DAOException, UserSessionException {
        // POLYMORPHISM: Pattern matching with switch expressions (Java 21+) performs runtime type
        // checking and dispatches to the appropriate registration method based on the actual type
        // of the UserBean parameter. This is a modern, type-safe alternative to instanceof chains.
        // The pattern matching automatically casts the userBean to the specific subtype (FanBean or
        // VenueManagerBean) in each case, allowing us to call type-specific registration methods.
        // The compiler ensures exhaustiveness - all possible UserBean subtypes must be handled.
        return switch (userBean) {
            case FanBean fanBean -> signUpFan(fanBean, autoLogin);
            case VenueManagerBean venueManagerBean -> signUpVenueManager(venueManagerBean, autoLogin);
            case null -> throw new IllegalArgumentException("User bean cannot be null");
            default -> throw new IllegalArgumentException("Invalid user type for signup: " + userBean.getClass().getName());
        };
    }

    /**
     * Stores user session information upon successful registration (if autoLogin is enabled).
     *
     * @param user The user for whom the session is to be stored
     * @throws UserSessionException If there is an error in starting a new session
     */
    @Override
    protected void storeUserSession(User user) throws UserSessionException {
        SessionManager.INSTANCE.login(user);
    }

    /**
     * Converts a User Model object to a UserBean for boundary layer consumption.
     *
     * @param user The User Model object to convert
     * @return A UserBean containing only data (no business logic)
     */
    private UserBean convertUserToBean(User user) {
        UserType userType = user.getUserType();

        if (userType == UserType.FAN) {
            Fan fan = (Fan) user;
            return new FanBean.Builder()
                    .username(fan.getUsername())
                    .fullName(fan.getFullName())
                    .gender(fan.getGender())
                    .type(userType.toString())
                    .favTeam(fan.getFavTeam())
                    .birthday(fan.getBirthday())
                    .build();
        } else if (userType == UserType.VENUE_MANAGER) {
            VenueManager manager = (VenueManager) user;
            return new VenueManagerBean.Builder()
                    .username(manager.getUsername())
                    .fullName(manager.getFullName())
                    .gender(manager.getGender())
                    .type(userType.toString())
                    .companyName(manager.getCompanyName())
                    .phoneNumber(manager.getPhoneNumber())
                    .build();
        }

        // Fallback to basic UserBean
        return new UserBean.Builder<>()
                .username(user.getUsername())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .type(userType.toString())
                .build();
    }
}
