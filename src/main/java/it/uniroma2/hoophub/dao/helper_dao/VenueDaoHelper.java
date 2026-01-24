package it.uniroma2.hoophub.dao.helper_dao;

import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for loading Venue dependencies.
 *
 * <p>Eliminates code duplication between VenueDaoCsv and VenueDaoMySql
 * by centralizing VenueManager retrieval logic.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class VenueDaoHelper {

    private static final Logger logger = Logger.getLogger(VenueDaoHelper.class.getName());

    private VenueDaoHelper() {
        // Utility class
    }

    /**
     * Loads a VenueManager by username.
     *
     * @param managerUsername the manager's username
     * @return the VenueManager entity
     * @throws DAOException if retrieval fails or manager not found
     */
    public static VenueManager loadVenueManager(String managerUsername) throws DAOException {
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        VenueManagerDao venueManagerDao = daoFactory.getVenueManagerDao();
        VenueManager venueManager = venueManagerDao.retrieveVenueManager(managerUsername);

        if (venueManager == null) {
            logger.log(Level.SEVERE, "VenueManager not found for venue mapping: {0}", managerUsername);
            throw new DAOException("VenueManager not found: " + managerUsername);
        }

        return venueManager;
    }
}