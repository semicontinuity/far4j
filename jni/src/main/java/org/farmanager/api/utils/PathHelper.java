package org.farmanager.api.utils;

public class PathHelper {

    public static String childPath(String path, String directory) {
        return "/".equals(path) ? '/' + directory : path + '/' + directory;
    }

    public static String parentPath(String path) {
        return path.substring(0, path.lastIndexOf('/'));
    }
}
