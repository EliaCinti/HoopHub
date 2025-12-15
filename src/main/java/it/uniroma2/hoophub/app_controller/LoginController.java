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
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.enums.UserType;

/**
 * LoginController manages the authentication process.
 * <p>
 * Refactored to be STATELESS. The state regarding failed attempts is delegated
 * to {@link LoginAttemptManager}. This class is now instantiated by the boundary controllers.
 * </p>
 */
public class LoginController extends AbstractController {

    /**
     * Public constructor. No Singleton pattern here.
     */
    public LoginController() {
        // Stateless controller
    }

    /**
     * Esegue il login completo.
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

    @Override
    protected void storeUserSession(User user) throws UserSessionException {
        SessionManager.INSTANCE.login(user);
    }

    private UserBean convertUserToBean(User user) {
        return switch (user) {
            case Fan fan -> new FanBean.Builder()
                    .username(fan.getUsername())
                    .fullName(fan.getFullName())
                    .gender(fan.getGender())
                    .type(UserType.FAN)
                    .favTeam(fan.getFavTeam())
                    .birthday(fan.getBirthday())
                    .build();

            case VenueManager vm -> new VenueManagerBean.Builder()
                    .username(vm.getUsername())
                    .fullName(vm.getFullName())
                    .gender(vm.getGender())
                    .type(UserType.VENUE_MANAGER)
                    .companyName(vm.getCompanyName())
                    .phoneNumber(vm.getPhoneNumber())
                    .build();

            case null -> throw new IllegalArgumentException("User cannot be null");
            default -> throw new IllegalStateException("Unexpected value: " + user);
        };
    }
}