package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.model.VenueManager;

/**
 * Abstract base class for application controllers requiring session management.
 *
 * <p>Defines a common contract for storing user sessions, implemented by
 * concrete controllers like {@link LoginController} and {@link SignUpController}.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public abstract class AbstractController {

    /**
     * Stores user session information after successful authentication or registration.
     *
     * @param user the user whose session needs to be stored
     * @throws UserSessionException if session storage fails (e.g., duplicate session)
     */
    protected abstract void storeUserSession(User user) throws UserSessionException;
    /**
     * Converts a User model to the appropriate UserBean subtype.
     *
     * @param user the user model to convert
     * @return corresponding {@link FanBean} or {@link VenueManagerBean}
     * @throws IllegalArgumentException if a user is null
     * @throws IllegalStateException if user type is unexpected
     */
    protected UserBean convertUserToBean(User user) {
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
            default -> throw new IllegalStateException("Unexpected user type: " + user.getClass().getName());
        };
    }
}