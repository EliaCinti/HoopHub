package it.uniroma2.hoophub.utilities;

import it.uniroma2.hoophub.enums.VenueType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Utility class to resolve venue images based on venue type.
 * <p>
 * Images are not persisted in the database - they are derived at runtime
 * based on the venue type. This keeps the persistence layer simple while
 * still providing visual differentiation in the UI.
 * </p>
 */
public class VenueImageResolver {

    private static final String IMAGES_BASE_PATH = "/it/uniroma2/hoophub/images/venues/";
    
    private static final Map<VenueType, String> TYPE_IMAGES = new EnumMap<>(VenueType.class);
    
    private static final String DEFAULT_IMAGE = "default_venue.png";

    static {
        // Map each venue type to its corresponding image
        TYPE_IMAGES.put(VenueType.SPORTS_BAR, "sports_bar.png");
        TYPE_IMAGES.put(VenueType.FAN_CLUB, "fan_club.png");
        TYPE_IMAGES.put(VenueType.RESTAURANT, "restaurant.png");
        TYPE_IMAGES.put(VenueType.PUB, "pub.png");
        // Add more mappings as needed when new VenueTypes are added
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private VenueImageResolver() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the full resource path to the image for a given venue type.
     *
     * @param type The venue type
     * @return The full resource path to the image (e.g., "/it/uniroma2/hoophub/images/venues/sports_bar.png")
     */
    public static String getImagePath(VenueType type) {
        if (type == null) {
            return IMAGES_BASE_PATH + DEFAULT_IMAGE;
        }
        String imageName = TYPE_IMAGES.getOrDefault(type, DEFAULT_IMAGE);
        return IMAGES_BASE_PATH + imageName;
    }

    /**
     * Gets just the image filename for a given venue type.
     *
     * @param type The venue type
     * @return The image filename (e.g., "sports_bar.png")
     */
    public static String getImageFilename(VenueType type) {
        if (type == null) {
            return DEFAULT_IMAGE;
        }
        return TYPE_IMAGES.getOrDefault(type, DEFAULT_IMAGE);
    }

    /**
     * Gets the base path for venue images.
     *
     * @return The base path for venue images
     */
    public static String getImagesBasePath() {
        return IMAGES_BASE_PATH;
    }
}
