package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.utilities.LoginAttemptManager;
import it.uniroma2.hoophub.model.Credentials;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.enums.UserType;

/**
 * Application controller handling user authentication.
 *
 * <p>Stateless controller that manages the complete login flow: rate limiting,
 * credential validation, profile loading, and session creation. Failed attempt
 * tracking is delegated to {@link LoginAttemptManager}.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see LoginAttemptManager
 * @see SessionManager
 */
public class LoginController extends AbstractController {

    /**
     * Creates a new stateless LoginController instance.
     */
    public LoginController() {
        // Stateless controller
    }

    /**
     * Performs complete user authentication.
     *
     * <p>Flow: rate limit check → credential validation → profile loading →
     * session creation → return user bean. On failure, records the attempt
     * and re-throws the exception.</p>
     *
     * @param bean credentials containing username and password
     * @return {@link UserBean} (either {@link FanBean} or {@link VenueManagerBean})
     * @throws DAOException if validation fails or user profile not found
     * @throws UserSessionException if rate limit exceeded or session error
     */
    public UserBean login(CredentialsBean bean) throws DAOException, UserSessionException {
        // 1. Rate Limiting Check (Delegato al Manager Singleton)
        LoginAttemptManager.getInstance().checkRateLimit();

        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        UserDao userDao = daoFactory.getUserDao();

        try {
            // 2. Bean -> Model (Credentials)
            Credentials credentials = new Credentials.Builder()
                    .username(bean.getUsername())
                    .password(bean.getPassword())
                    .build();

            // 3. Validazione
            UserType userType = userDao.validateUser(credentials);

            // 4. Caricamento Profilo
            User user = null;
            if (userType == UserType.FAN) {
                user = daoFactory.getFanDao().retrieveFan(credentials.getUsername());
            } else if (userType == UserType.VENUE_MANAGER) {
                user = daoFactory.getVenueManagerDao().retrieveVenueManager(credentials.getUsername());
            }

            if (user == null) {
                throw new DAOException("User profile corrupted or not found: " + credentials.getUsername());
            }

            // 5. Successo: Reset tentativi (Delegato) e Login in Sessione
            LoginAttemptManager.getInstance().resetFailedAttempts();
            storeUserSession(user);

            // 6. Restituzione Bean
            return convertUserToBean(user);

        } catch (DAOException | IllegalArgumentException e) {
            // Fallimento: Registra tentativo (Delegato) e rilancia
            LoginAttemptManager.getInstance().recordFailedAttempt();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * Converts the User model to UserBean and stores it in SessionManager.
     */
    @Override
    protected void storeUserSession(User user) throws UserSessionException {
        UserBean userBean = convertUserToBean(user);
        SessionManager.INSTANCE.login(userBean);
    }
}