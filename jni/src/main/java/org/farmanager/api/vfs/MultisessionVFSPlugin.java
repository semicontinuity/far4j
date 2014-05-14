package org.farmanager.api.vfs;

import org.farmanager.api.AbstractPlugin;

import java.io.File;

/**
 * This class is a base for all plugins that can have a number of stored "sessions"
 * (similar to WinSCP plugin).
 * The sessions are stored in individual files in the folder "sessions" under plugins' folder.
 * These files are regular property files. When the session is "opened",
 * {@link GenericSessionListPanelContentProvider.Listener#openSession(java.util.Properties)}
 * is called, and the contents of this file is passed to it.
 */
public abstract class MultisessionVFSPlugin extends AbstractPlugin {

    public final File pluginLocalSettingsFolder() {
        final String farProfileEnv = System.getenv().get("FARLOCALPROFILE");
        final File farProfileFolder = new File(farProfileEnv);
        if (!farProfileFolder.isDirectory())
            throw new IllegalStateException("%FARLOCALPROFILE% folder does not exist!");

        final File pluginHome = getHome();
        final String pluginName = pluginHome.getName();

        final File far4jSettingsFolder = new File(farProfileFolder, "far4j");
        final File pluginSettingsFolder = new File(far4jSettingsFolder, pluginName);

        if (!pluginSettingsFolder.exists() || !pluginSettingsFolder.isDirectory())
            throw new IllegalStateException("Cannot find plugin settings folder " + pluginSettingsFolder);
        return pluginSettingsFolder;
    }
}
