package org.farmanager.plugins.jdbc;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.farmanager.api.AbstractPlugin;
import org.farmanager.api.AbstractPluginInstance;
import org.farmanager.api.jni.UsedFromNativeCode;
import org.farmanager.api.vfs.MultisessionVFSPlugin;

@UsedFromNativeCode
public class JDBCPlugin extends MultisessionVFSPlugin {


    static Logger LOGGER = Logger.getLogger(JDBCPlugin.class);

    private static DiscMenuItemInfo[] DISC_MENU_INFO = new DiscMenuItemInfo[]{
            new DiscMenuItemInfo(0, "JDBC")
    };

    public JDBCPlugin() {
        AbstractPlugin.LOGGER.info("@ <init>!");
    }

    @Override
    protected void init() {
        AbstractPlugin.LOGGER.info("@ init!");
        LOGGER.info("@ init!!");
    }

    @Override
    public AbstractPluginInstance createInstance() {
        LOGGER.warn("@ createInstance...");
        final JDBCPluginInstance jdbcPluginInstance = new JDBCPluginInstance(this);
        LOGGER.warn("@ createInstance " + jdbcPluginInstance);
        return jdbcPluginInstance;
    }

    // =========================================================================
    // GetPluginInfo
    // =========================================================================


    @Override
    public String[] getPluginConfigMenu() {
        return new String[]{"JDBC"};
    }

    /**
     * @return a list of strings that will go to Plugin commands menu
     */
    @Override
    public String[] getPluginMenu() {
        return new String[]{"JDBC"};
    }

    @Override
    public String getCommandPrefix() {
        return "jdbc";
    }

    @Override
    public DiscMenuItemInfo[] getDiscMenuInfo() {
        return DISC_MENU_INFO;
    }

    public final File templatesFolder() {
        final File templatesDir = new File(getHome(), "templates");
        if (!templatesDir.exists() || !templatesDir.isDirectory())
            throw new IllegalStateException("Cannot find the folder with a list of templates!");
        return templatesDir;
    }

}
