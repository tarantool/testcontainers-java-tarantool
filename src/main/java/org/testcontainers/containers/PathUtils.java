package org.testcontainers.containers;

/**
 * Utils class for path normalization.
 *
 * @author Vladimir Rogach
 */
public class PathUtils {

    private PathUtils() {
    }

    /**
     * Removes leading slash under windows from "/C:/work" so
     * the path will match the format expected by docker.
     *
     * @param binding path
     * @return normalized path for docker
     */
    public static String normalizeBindingPath(String path) {
        if(path.startsWith("/") && path.length() > 3 && path.charAt(2) == ':' ) {
            return path.substring(1);
        }
        return path;
    }
}
