package org.farmanager.api.jni;

/**
 * Corresponds to struct PluginInfo
 */
public class PluginInfo {

    /** Corresponds to the type PLUGIN_FLAGS; {@linkplain #flags} is set to a combination of values. */
    public static interface Flags {
        long
        PF_PRELOAD        = 0x0000000000000001L,
        PF_DISABLEPANELS  = 0x0000000000000002L,
        PF_EDITOR         = 0x0000000000000004L,
        PF_VIEWER         = 0x0000000000000008L,
        PF_FULLCMDLINE    = 0x0000000000000010L,
        PF_DIALOG         = 0x0000000000000020L,
        PF_NONE           = 0;
    }

    @UsedFromNativeCode
    private long flags;

    @UsedFromNativeCode
    private PluginMenuItem[] diskMenu;

    @UsedFromNativeCode
    private PluginMenuItem[] pluginMenu;

    @UsedFromNativeCode
    private PluginMenuItem[] pluginConfig;

    @UsedFromNativeCode
    private String commandPrefix;


    public void setFlags(long flags) { this.flags = flags; }

    public void setDiskMenu(PluginMenuItem[] diskMenu) { this.diskMenu = diskMenu; }

    public void setPluginMenu(PluginMenuItem[] pluginMenu) { this.pluginMenu = pluginMenu; }

    public void setPluginConfig(PluginMenuItem[] pluginConfig) { this.pluginConfig = pluginConfig; }

    public void setCommandPrefix(String commandPrefix) { this.commandPrefix = commandPrefix; }
}
