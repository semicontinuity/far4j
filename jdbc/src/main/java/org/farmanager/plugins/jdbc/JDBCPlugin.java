package org.farmanager.plugins.jdbc;

import java.io.File;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.farmanager.api.AbstractPluginInstance;
import org.farmanager.api.jni.PluginInfo;
import org.farmanager.api.jni.PluginMenuItem;
import org.farmanager.api.jni.UsedFromNativeCode;
import org.farmanager.api.vfs.MultisessionVFSPlugin;

@UsedFromNativeCode
public class JDBCPlugin extends MultisessionVFSPlugin {

    static Logger LOGGER = Logger.getLogger(JDBCPlugin.class);

    private static DiscMenuItemInfo[] DISC_MENU_INFO = new DiscMenuItemInfo[]{
            new DiscMenuItemInfo(0, "JDBC")
    };

    @Override
    public AbstractPluginInstance createInstance() throws Exception {
        return new JDBCPluginInstance(this);
    }

    // =========================================================================
    // GetPluginInfoW
    // =========================================================================

    @Override
    public PluginInfo getPluginInfo() {
        final PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setDiskMenu(new PluginMenuItem[] {
                new PluginMenuItem(UUID.randomUUID(), "Database")
        });
        pluginInfo.setCommandPrefix("db");
        return pluginInfo;
    }

    @Override
    public DiscMenuItemInfo[] getDiscMenuInfo() {
        return DISC_MENU_INFO;
    }

    public final File templatesFolder() {
        final File templatesDir = new File(pluginSettingsFolder(), "templates");
        if (!templatesDir.exists() || !templatesDir.isDirectory()) {
            throw new IllegalStateException("Cannot find the folder with a list of templates!");
        }
        return templatesDir;
    }
}
