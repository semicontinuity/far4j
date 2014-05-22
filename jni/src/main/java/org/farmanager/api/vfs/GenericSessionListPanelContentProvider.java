package org.farmanager.api.vfs;

import org.apache.log4j.Logger;
import org.farmanager.api.AbstractPlugin;
import org.farmanager.api.PanelMode;
import static org.farmanager.api.PanelModeId.BRIEF;
import org.farmanager.api.PluginPanelItem;
import static org.farmanager.api.jni.PanelColumnType.ID;

import org.farmanager.api.messages.Messages;
import org.farmanager.api.panels.NamedColumnDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * This is a plugin content provider, that provides a list of plugin's "sessions",
 *
 * @see MultisessionVFSPlugin
 */
public class GenericSessionListPanelContentProvider extends AbstractPanelContentProvider
{
    private static final Logger LOGGER = Logger.getLogger (GenericSessionListPanelContentProvider.class);

    protected final MultisessionVFSPlugin plugin;
    private static PanelMode[] PANEL_MODES;

    private Listener listener;
    private String title;
    private File rootSessionsFolder;
    private File rootSessionsFolder1;
//    private File currentSessionsFolder;

    private List<String> currentPath = new ArrayList<String>();


    public GenericSessionListPanelContentProvider (
            final MultisessionVFSPlugin plugin,
            final Listener listener,
            final String title)
    {
        this.title = title;
        this.plugin = plugin;
        this.listener = listener;
        this.rootSessionsFolder = new File(plugin.pluginSettingsFolder(), "sessions");
        this.rootSessionsFolder1 = rootSessionsFolder;
//        this.rootSessionsFolder1 = new File("c:");
//        this.rootSessionsFolder = new File("c:");
//        this.currentSessionsFolder = rootSessionsFolder1;
    }

    public PanelMode[] getPanelModes () {
        if (PANEL_MODES == null)
        {
            PANEL_MODES = new PanelMode[1];
            PANEL_MODES[BRIEF] = new PanelMode (
                new NamedColumnDescriptor[]{
                    new NamedColumnDescriptor ("Session name", ID, 0),
                },
                false,
                null
            );    // TODO: is it ok to have 1 mode??
        }
        return PANEL_MODES;
    }

    public int getStartPanelMode ()
    {
        return '0';
    }

    public String getPanelTitle ()
    {
        return title;
    }

    /** @param opMode a combination of OPM_* flags */
    public PluginPanelItem[] getFindData (final int opMode)
    {
        LOGGER.info ("opMode = " + opMode);

        final File currentFolder = new File(rootSessionsFolder, concat(currentPath, '/'));
        LOGGER.debug("currentFolder: " + currentFolder);
        final String[] sessions = /*currentSessionsFolder.list()*/currentFolder.list();
        final PluginPanelItem[] pluginPanelItems = new PluginPanelItem[sessions.length];
        for (int i = 0; i < sessions.length; i++)
        {
            String session = sessions[i];
            final PluginPanelItem pluginPanelItem = new PluginPanelItem ();
            pluginPanelItem.cFileName = session;
            pluginPanelItem.dwFileAttributes = PluginPanelItem.FILE_ATTRIBUTE_DIRECTORY;
            pluginPanelItems[i] = pluginPanelItem;
        }
        return pluginPanelItems;
    }


    public void setDirectory(final String directory) {
        LOGGER.info("setDirectory " + directory);
        if ("..".equals (directory))
        {
//            LOGGER.info("currentSessionsFolder=" + currentSessionsFolder);
//            LOGGER.info("rootSessionsFolder=" + rootSessionsFolder1);
//            LOGGER.info("?" + currentSessionsFolder.equals(rootSessionsFolder1));
            if (/*currentSessionsFolder.equals(rootSessionsFolder1)*/currentPath.isEmpty()) {
                AbstractPlugin.closePlugin ();
            }
            else {
                currentPath.remove(currentPath.size() - 1);
//                currentSessionsFolder = currentSessionsFolder.getParentFile();
            }
            return;
        }

        try {
            currentPath.addAll (Arrays.asList(directory.split("/")));
            LOGGER.debug("cp: " + currentPath);
//            final File target = new File(currentSessionsFolder, directory);
            final File target = new File(rootSessionsFolder, concat(currentPath, '/'));
            if (target.isFile()) {
                LOGGER.debug("Opening session");
                currentPath.remove(currentPath.size() - 1); // switching filesystem...
                listener.openSession(loadSession(target));
            }
            else {
                LOGGER.debug("Opening sublist");
//                currentSessionsFolder = target;
            }
        }
        catch (Exception e) {
            Messages.showException(e);
        }
        catch (NoClassDefFoundError x) {
            LOGGER.fatal (x, x);
        }
    }

    protected Properties loadSession(File target) throws IOException {
        final Properties properties = new Properties();
        final FileInputStream fileInputStream = new FileInputStream (target);
        properties.load (fileInputStream);
        fileInputStream.close ();
        return properties;
    }


    public String getCurrentDirectory() {
        return concat(currentPath, '/');
//        return currentSessionsFolder.getPath();
    }


    public static interface Listener {
        /** Invoked when a user wants to open a session for a session list panel */
        void openSession(final Properties properties) throws Exception;
    }


    public static String concat(final Collection<? extends CharSequence> words, final char delimiter) {
        final StringBuilder wordList = new StringBuilder();
        if (words.size() > 0) {
            for (CharSequence word : words) {
                wordList.append(word).append(delimiter);
            }
            wordList.setLength(wordList.length() - 1);
        }
        return wordList.toString();
    }
}
