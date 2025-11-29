package it.uniroma2.hoophub.dao.helper_dao;

import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class to eliminate code duplication between VenueDaoCsv and VenueDaoMySql.
 * Centralizes the logic for retrieving VenueManager dependencies.
 */
public class VenueDaoHelper {

    private static final Logger logger = Logger.getLogger(VenueDaoHelper.class.getName());

    private VenueDaoHelper() {
        // Utility class
    }

    /**
     * Loads a VenueManager by username using the DAO Factory.
     * Logs an error and throws an exception if the manager is not found.
     *
     * @param managerUsername The username of the venue manager to retrieve
     * @return The populated VenueManager object
     * @throws DAOException If retrieval fails or manager is not found
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
