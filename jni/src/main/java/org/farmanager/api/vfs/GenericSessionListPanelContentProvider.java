package org.farmanager.api.vfs;

import org.apache.log4j.Logger;
import org.farmanager.api.AbstractPlugin;
import org.farmanager.api.PanelMode;
import static org.farmanager.api.PanelModeId.BRIEF;
import org.farmanager.api.PluginPanelItem;
import static org.farmanager.api.jni.PanelColumnType.ID;
import org.farmanager.api.panels.NamedColumnDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
    private File currentSessionsFolder;


    public GenericSessionListPanelContentProvider (
        MultisessionVFSPlugin plugin,
        Listener listener,
        String title)
    {
        this.title = title;
        this.plugin = plugin;
        this.listener = listener;
        this.rootSessionsFolder = plugin.sessionsFolder();
        this.currentSessionsFolder = rootSessionsFolder;
    }

    public PanelMode[] getPanelModes ()
    {
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

        final String[] sessions = currentSessionsFolder.list ();
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


    public void setDirectory (String directory)
    {
        if ("..".equals (directory))
        {
            LOGGER.info("currentSessionsFolder=" + currentSessionsFolder);
            LOGGER.info("rootSessionsFolder=" + rootSessionsFolder);
            LOGGER.info("?" + currentSessionsFolder.equals(rootSessionsFolder));
            if (currentSessionsFolder.equals(rootSessionsFolder))
            {
                AbstractPlugin.closePlugin ();
            }
            else
            {
                currentSessionsFolder = currentSessionsFolder.getParentFile();
            }
            return;
        }

        try
        {
            final File target = new File (currentSessionsFolder, directory);
            if (target.isFile()) {
                LOGGER.debug("Opening session");
                listener.openSession (loadSession(target));
            }
            else
            {
                LOGGER.debug("Opening sublist");
                currentSessionsFolder = target;
            }
        }
        catch (Exception e)
        {
            LOGGER.error (e, e);
        }
        catch (NoClassDefFoundError x)
        {
            LOGGER.fatal (x, x);
        }
    }

    protected Properties loadSession(File target) throws IOException {
        final Properties properties = new Properties ();
        final FileInputStream fileInputStream = new FileInputStream (target);
        properties.load (fileInputStream);
        fileInputStream.close ();
        return properties;
    }


    public String getCurrentDirectory()
    {
        return currentSessionsFolder.getPath();
    }

    public static interface Listener
    {
        /** Invoked when a used wans to open a session for a session list panel */
        void openSession (final Properties properties) throws Exception;
    }
}
