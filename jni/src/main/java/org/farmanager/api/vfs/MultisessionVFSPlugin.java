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

}
