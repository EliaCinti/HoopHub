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
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.utilities.PasswordUtils;

import java.util.ArrayList;

/**
 * SignUpController manages the registration process for new users.
 * <p>
 * Refactored to be STATELESS and instantiable via constructor.
 * </p>
 */
public class SignUpController extends AbstractController {

    /**
     * Public constructor.
     */
    public SignUpController() {
        // Stateless controller
    }

    // ... (Il metodo signUpFan rimane identico a prima) ...
    private UserBean signUpFan(FanBean fanBean, boolean autoLogin) throws DAOException, UserSessionException {
        // ... codice invariato ...
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

    // ... (Il metodo signUpVenueManager rimane identico a prima) ...
    private UserBean signUpVenueManager(VenueManagerBean vmBean, boolean autoLogin) throws DAOException, UserSessionException {
        // ... codice invariato ...
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

    // ... (Il metodo signUp rimane identico a prima) ...
    public UserBean signUp(UserBean userBean, boolean autoLogin) throws DAOException, UserSessionException {
        return switch (userBean) {
            case FanBean fanBean -> signUpFan(fanBean, autoLogin);
            case VenueManagerBean venueManagerBean -> signUpVenueManager(venueManagerBean, autoLogin);
            case null -> throw new IllegalArgumentException("User bean cannot be null");
            default -> throw new IllegalArgumentException("Invalid user type for signup: " + userBean.getClass().getName());
        };
    }

    @Override
    protected void storeUserSession(User user) throws UserSessionException {
        SessionManager.INSTANCE.login(user);
    }

    // ... (Il metodo convertUserToBean rimane identico a prima) ...
    private UserBean convertUserToBean(User user) {
        // ... codice invariato ... (metodo già corretto nella versione precedente)
        UserType userType = user.getUserType();
        // ... switch case implementation ...
        return switch (user) {
            case Fan fan -> new FanBean.Builder()
                    .username(fan.getUsername())
                    .fullName(fan.getFullName())
                    .gender(fan.getGender())
                    .type(userType)
                    .favTeam(fan.getFavTeam())
                    .birthday(fan.getBirthday())
                    .build();
            case VenueManager manager -> new VenueManagerBean.Builder()
                    .username(manager.getUsername())
                    .fullName(manager.getFullName())
                    .gender(manager.getGender())
                    .type(userType)
                    .companyName(manager.getCompanyName())
                    .phoneNumber(manager.getPhoneNumber())
                    .build();
            default -> throw new IllegalArgumentException("Invalid user type"); // Semplificato per brevità qui
        };
    }
}