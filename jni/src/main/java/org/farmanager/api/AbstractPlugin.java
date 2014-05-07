package org.farmanager.api;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.farmanager.api.jni.UsedFromNativeCode;
import org.farmanager.api.jni.ReturnCodes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Represents a FAR plugin (not a plugin instance)
 *
 * The concrete subclass of this class is instantiated from native code.
 * The name of concrete subclass is specified in slash-separated format
 * in the file %PLUGIN_HOME%/plugin-class
 *
 * Contains thin JNI wrappers around FAR API.
 */
@SuppressWarnings({"ClassWithTooManyMethods"})
@UsedFromNativeCode
public abstract class AbstractPlugin
{
    static {

        DOMConfigurator.configure(
                new File("C:\\Users\\igorkarpov\\AppData\\Local\\Temp", "log4j.xml").getAbsolutePath());
    }
    public static Logger LOGGER = Logger.getLogger(AbstractPlugin.class);


    /** Plugin module - far4j dll file */
    protected File module;

    /** Plugin folder */
    protected File home;

    public static AbstractPlugin instance() throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, IOException {
        LOGGER.info("@ instance");
        return (AbstractPlugin) Class.forName("org.farmanager.plugins.jdbc.JDBCPlugin").newInstance();
    }

    /**
     * Get the plugin's home directory
     * @return Home directory of plugin, i.e. a folder under %FARHOME%\Plugins
     */
    public File getHome () {
        return home;
    }


    /**
     * This method is called from OpenW to create a new plugin instance.
     * Corresponds to "OpenW" exported function.
     * @return an instance of plugin created.
     */
    @UsedFromNativeCode
    public abstract AbstractPluginInstance createInstance();


    /**
     * Set module name
     * FAR calls SetStartupInfo function and passes module name in PluginStartupInfo structure.
     * Then JNI layer calls this method to make it available to java code.
     * TODO: can be static... or passed as constructors' parameter..
     * @param moduleName    module name (the full path to the far4j DLL file)
     */
    @UsedFromNativeCode
    protected final void setModuleName (final String moduleName)
    {
        LOGGER.info("MN:" + moduleName);
        this.module = new File (moduleName);
        this.home = module.getParentFile ();

        // Although dll is loaded, we call this
        // (perhaps some important linkage occurs)
        // Without this we will not be able to call native methods.
        System.load (moduleName);
        init ();
    }

    /**
     * This method is called during plugin initialization.
     * In this method the information passed by FAR in SetStartupInfo is already available,
     * and it is possible to call native methods.
     */
    protected void init () {
        // No futher initialization by default
    }

    // =========================================================================
    // GetPluginInfo
    // =========================================================================

    // FLAGS
    /**
     * Disables plugin parameters caching and forces FAR to always load the plugin when starting.
     * Must be specified if it is necessary to change lines
     * in "Disks", "Plugins" or "Plugins configuration" menus dynamically.
     * This flag decreases efficiency of memory usage.
     */
    public static final int PF_PRELOAD = 0x0001;

    /** Do not show the plugin in the "Plugin commands" menu called from panels. */
    public static final int PF_DISABLEPANELS = 0x0002;

    /** Show the plugin in the "Plugin commands" menu called from FAR editor. */
    public static final int PF_EDITOR = 0x0004;

    /** Show the plugin in the "Plugin commands" menu called from FAR viewer. */
    public static final int PF_VIEWER = 0x0008;

    /**
     * Forces FAR to pass to the plugin the full command line with the prefix.
     * It is necessary to use this flag when a plugin can handle multiple command line prefixes.
     */
    public static final int PF_FULLCMDLINE = 0x0010;


    /**
     * FAR calls exported function GetPluginInfo, that should fill PluginInfo structure.
     * The exported function calls this method to get value for "Flags" field.
     *
     * @return Combination of the PF_* constants to be propagated into PluginInfo structure
     */
    @UsedFromNativeCode
    public int getFlags ()
    {
        // no special flags by default
        return 0;
    }

    /**
     * FAR calls exported function GetPluginInfo, that should fill PluginInfo structure.
     * The exported function calls this method
     * to get value for "PluginMenuStrings" and "PluginMenuStringsNumber" fields.
     *
     * @return a list of strings that will go to Plugin commands menu
     */
    @UsedFromNativeCode
    public String[] getPluginMenu ()
    {
        // No plugin menu by default
        return null;
    }


    /** @return a list of strings that will go to Plugin config menu */
    @UsedFromNativeCode
    public String[] getPluginConfigMenu ()
    {
        // No plugin config menu by default
        return null;
    }

    @UsedFromNativeCode
    public String getCommandPrefix ()
    {
        // No command prefix by default
        return null;
    }


    /** FAR disc menu info */
    protected static class DiscMenuItemInfo
    {
        public int number;
        public String name;

        public DiscMenuItemInfo (int number, String name)
        {
            this.number = number;
            this.name = name;
        }
    }

    public DiscMenuItemInfo[] getDiscMenuInfo ()
    {
        // No disk menu info by default
        return null;
    }

    // =========================================================================
    // Configure
    // =========================================================================

    /**
     * Invoked from FAR JNI layer when a plugin is being configured
     * via Options|Plugins configuration.
     * Corresponds to "Configure" exported function.
     * Concrete plugins can show a configuration dialog in overriden method.
     * @return If the function succeeds, the return value must be TRUE;
     * in this case FAR updates the panels.
     * If the configuration is canceled by user, FALSE should be returned.
     * @param itemNumber    The number of selected item in the list of items exported
     * by this plugin to the Plugin configuration menu.
     * @see org.farmanager.api.jni.ReturnCodes
     */
    @UsedFromNativeCode
    public int configure (final int itemNumber)
    {
        // Do nothing by default
        return ReturnCodes.FALSE;
    }

    // =========================================================================
    // FAR service functions
    // FAR provides references to service functions in PluginStartupInfo struct
    // =========================================================================

    /**
     * Saves all screen.
     * A wrapper around SaveScreen service function.
     * @return The return value is a handle that can be passed to restoreScreen.
     * All handles allocated by saveScreen must be passed to restoreScreen
     * to avoid memory leaks.
     */
    public static int saveScreen () {
        return saveScreen (0, 0, -1, -1);
    }

    /**
     * A wrapper around SaveScreen service function.
     *
     * @param x1 Screen area coordinate X1.
     *           If X2 or Y2 is equal to -1,
     *           they are replaced with screen right or screen bottom coordinate correspondingly.
     *           So SaveScreen(0,0,-1,-1) will save the entire screen.
     * @param y1 Screen area coordinate Y1.
     * @param x2 Screen area coordinate X2.
     * @param y2 Screen area coordinate Y1.
     * @return The return value is a handle that can be passed to restoreScreen.
     * All handles allocated by saveScreen must be passed to restoreScreen
     * to avoid memory leaks.
     */
    public static native int saveScreen(final int x1, final int y1, final int x2, final int y2);

    public static native void restoreScreen(final int hScreen);

    public static native int message (
        final int flags, final String helpTopic, final String[] items, final int buttonsNumber);


    /**
     * Shows a dialog.
     * Thin wrapper around Dialog function from FAR API.
     *
     * @return either -1, if the user cancelled the dialog,
     * or the index of the selected dialog item in the Item array.
     * @param x1    Dialog coordinate X1.
     * You can specify coordinates directly or set X1 and Y1 to -1 and X2 and Y2
     * to the dialog width and height.
     * In the latter case the dialog will be automatically centered.
     * @param y1    Dialog coordinate Y1.
     * @param x2    Dialog coordinate X2.
     * @param y2    Dialog coordinate Y1.
     * @param helpTopic Help topic associated with the dialog.
     * It can be <code>null</code> if help is not required.
     * @param initDialogItems   array of InitDialogItem structures,
     * describing UI widgets of the dialog
     */
    public static native int dialog (
        final int x1,
        final int y1,
        final int x2,
        final int y2,
        final String helpTopic,
        final InitDialogItem[] initDialogItems
    );

    /**
     * Retrieves selected item
     * calls Info.Control(INVALID_HANDLE_VALUE, FCTL_GETPANELINFO, ..) and returns
     * SelectedItems
     *
     * @return selected item
     */
    public static native String getSelectedItem ();

    public static native int getSelectedItemCrc32 ();
    
    public static native String[] getSelectedItems ();

    public static native int getCurrentItem ();

    public static native String getAnotherPanelDirectory ();

    public static native String getCommandLine ();

    public static native void updatePanel ();

    public static native void redrawPanel ();

    public static native void closePlugin ();
}
