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
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.utilities.PasswordUtils;

import java.util.ArrayList;

/**
 * Application controller for user registration.
 *
 * <p>Stateless controller handling Fan and VenueManager registration with
 * password hashing and optional auto-login after successful signup.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see PasswordUtils
 */
public class SignUpController extends AbstractController {

    /**
     * Creates a new stateless SignUpController instance.
     */
    public SignUpController() {
        // Stateless controller
    }

    /**
     * Registers a new user based on the bean type.
     *
     * @param userBean either {@link FanBean} or {@link VenueManagerBean}
     * @param autoLogin if true, automatically logs in the user after registration
     * @return the registered user as {@link UserBean}
     * @throws DAOException if registration or retrieval fails
     * @throws UserSessionException if auto-login session creation fails
     * @throws IllegalArgumentException if userBean is null or invalid type
     */
    public UserBean signUp(UserBean userBean, boolean autoLogin) throws DAOException, UserSessionException {
        if (userBean == null) {
            throw new IllegalArgumentException("User bean cannot be null");
        }

        return switch (userBean) {
            case FanBean fanBean -> signUpFan(fanBean, autoLogin);
            case VenueManagerBean venueManagerBean -> signUpVenueManager(venueManagerBean, autoLogin);
            default ->
                    throw new IllegalArgumentException("Invalid user type for signup: " + userBean.getClass().getName());
        };
    }

    /**
     * Checks if a username is already taken.
     *
     * @param username the username to check
     * @return true if taken, false if available
     * @throws UserSessionException if database check fails
     * @throws IllegalArgumentException if username is null or blank
     */
    public boolean isUsernameTaken(String username) throws UserSessionException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        try {
            DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
            return daoFactory.getUserDao().isUsernameTaken(username);
        } catch (DAOException e) {
            throw new UserSessionException("Unable to verify username availability");
        }
    }

    /**
     * Registers a new Fan user.
     */
    private UserBean signUpFan(FanBean fanBean, boolean autoLogin) throws DAOException, UserSessionException {
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        FanDao fanDao = daoFactory.getFanDao();
        String hashedPassword = PasswordUtils.hashPassword(fanBean.getPassword());

        Fan newFan = new Fan.Builder()
                .username(fanBean.getUsername())
                .password(hashedPassword)
                .fullName(fanBean.getFullName())
                .gender(fanBean.getGender())
                .favTeam(fanBean.getFavTeam())
                .birthday(fanBean.getBirthday())
                .build();

        fanDao.saveFan(newFan);
        Fan registeredFan = fanDao.retrieveFan(fanBean.getUsername());

        if (registeredFan == null) throw new DAOException("Fan saved but not retrieved.");

        if (autoLogin) storeUserSession(registeredFan);

        return convertUserToBean(registeredFan);
    }

    /**
     * Registers a new VenueManager user.
     */
    private UserBean signUpVenueManager(VenueManagerBean vmBean, boolean autoLogin) throws DAOException, UserSessionException {
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        VenueManagerDao vmDao = daoFactory.getVenueManagerDao();
        String hashedPassword = PasswordUtils.hashPassword(vmBean.getPassword());

        VenueManager newManager = new VenueManager.Builder()
                .username(vmBean.getUsername())
                .password(hashedPassword)
                .fullName(vmBean.getFullName())
                .gender(vmBean.getGender())
                .companyName(vmBean.getCompanyName())
                .phoneNumber(vmBean.getPhoneNumber())
                .managedVenues(new ArrayList<>())
                .build();

        vmDao.saveVenueManager(newManager);
        VenueManager registeredManager = vmDao.retrieveVenueManager(vmBean.getUsername());

        if (registeredManager == null) throw new DAOException("VenueManager saved but not retrieved.");

        if (autoLogin) storeUserSession(registeredManager);

        return convertUserToBean(registeredManager);
    }

    /**
     * {@inheritDoc}
     * Converts User model to UserBean and stores in SessionManager.
     */
    @Override
    protected void storeUserSession(User user) throws UserSessionException {
        UserBean userBean = convertUserToBean(user);
        SessionManager.INSTANCE.login(userBean);
    }
}