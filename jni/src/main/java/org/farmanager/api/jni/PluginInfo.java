package org.farmanager.api.jni;

/**
 * Corresponds to struct PluginInfo
 */
public class PluginInfo {

    /** Corresponds to the type PLUGIN_FLAGS; {@linkplain #flags} is set to a combination of values. */
    public static interface Flags {
        long

        /**
         * Disables plugin parameters caching and forces FAR to always load the plugin when starting.
         * Must be specified if it is necessary to change lines
         * in "Disks", "Plugins" or "Plugins configuration" menus dynamically.
         * This flag decreases efficiency of memory usage.
         */
        PF_PRELOAD        = 0x0000000000000001L,

        /** Do not show the plugin in the "Plugin commands" menu called from panels. */
        PF_DISABLEPANELS  = 0x0000000000000002L,

        /** Show the plugin in the "Plugin commands" menu called from FAR editor. */
        PF_EDITOR         = 0x0000000000000004L,

        /** Show the plugin in the "Plugin commands" menu called from FAR viewer. */
        PF_VIEWER         = 0x0000000000000008L,

        /**
         * Forces FAR to pass to the plugin the full command line with the prefix.
         * It is necessary to use this flag when a plugin can handle multiple command line prefixes.
         */
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

    /**
     * A colon-separated string of command line prefixes.
     */
    @UsedFromNativeCode
    private String commandPrefix;


    public void setFlags(long flags) { this.flags = flags; }

    public void setDiskMenu(PluginMenuItem[] diskMenu) { this.diskMenu = diskMenu; }

    public void setPluginMenu(PluginMenuItem[] pluginMenu) { this.pluginMenu = pluginMenu; }

    public void setPluginConfig(PluginMenuItem[] pluginConfig) { this.pluginConfig = pluginConfig; }

    public void setCommandPrefix(String commandPrefix) { this.commandPrefix = commandPrefix; }
}
