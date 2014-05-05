package org.farmanager.api;

import org.farmanager.api.jni.UsedFromNativeCode;
import org.farmanager.api.jni.FarInfoPanelLine;


/**
 * Corresponds to open plugin instance.
 * Instances of this class are created for every new plugin instance
 * by implementations of {@link AbstractPlugin#createInstance(int)}
 * TODO: All low-level code should be moved to jni package!
 */
@UsedFromNativeCode
public abstract class AbstractPluginInstance
{
    /**
     * Initialized to command line, if plugin was initialized form the command line
     */
    @UsedFromNativeCode
    protected String commandLine;

    /**
     * Initialize this plugin instance.
     * Not all data are available in constructor call, e.g. commandLine
     */
    @UsedFromNativeCode
    public void init ()
    {
        // Do nothing by default.
    }

    // =========================================================================
    // ClosePlugin
    // =========================================================================

    /**
     * Close this plugin instance.
     * Concrete plugin instances should release resources.
     * <p/>
     * FAR calls ClosePlugin exported function, ClosePlugin calls this method
     * of the proper plugin instance object.
     */
    @UsedFromNativeCode
    public void close ()
    {
        // Do nothing by default.
    }

    // =========================================================================
    // GetFindData
    // =========================================================================

    /**
     * Get the list of files in the current directory
     * of the file system emulated by the plugin.
     * <p/>
     * FAR calls GetFindData exported function, GetFindData calls this method
     * of the proper plugin instance object.
     *
     * @param opMode a combination of OPM_* flags
     *               (use {@link org.farmanager.api.jni.OperationModeFlags}
     */
    @UsedFromNativeCode
    public PluginPanelItem[] getFindData (int opMode)
    {
        // No panel data by default
        return null;
    }

    // =========================================================================
    // GetOpenPluginInfo
    // =========================================================================


    @UsedFromNativeCode
    public int getFlags ()
    {
        // no special flags by default
        return 0;
    }

    /**
     * Related to FAR exported function GetOpenPluginInfo
     *
     * @return Name of the file used to emulate the file system.
     *         If plugin does not emulate a file system based on a file, set this variable to NULL.
     */
    @UsedFromNativeCode
    public String getHostFile ()
    {
        return null;
    }

    /** Related to FAR exported function GetOpenPluginInfo */
    @UsedFromNativeCode
    public String getCurDir ()
    {
        return null;
    }

    /** Related to FAR exported function GetOpenPluginInfo */
    @UsedFromNativeCode
    public String getFormat ()
    {
        return null;
    }

    /** Related to FAR exported function GetOpenPluginInfo */
    @UsedFromNativeCode
    public String getPanelTitle ()
    {
        return null;
    }

    /** Related to FAR exported function GetOpenPluginInfo */
    @UsedFromNativeCode
    public PanelMode[] getPanelModes ()
    {
        return null;
    }

    /** Related to FAR exported function GetOpenPluginInfo */
    @UsedFromNativeCode
    public int getStartPanelMode ()
    {
        return 0;
    }

    /** Related to FAR exported function GetOpenPluginInfo */
    @UsedFromNativeCode
    public FarInfoPanelLine[] getInfoPanelLines ()
    {
        // no info panel lines by default
        return null;
    }

    // =========================================================================
    // SetDirectory
    // =========================================================================

    /**
     * This method is called to set the current directory
     * in the file system emulated by the plugin.
     * FAR calls exported function SetDirectory, SetDirectory calls this method
     * of the proper plugin instance object.
     */
    @UsedFromNativeCode
    public void setDirectory (final String directory)
    {
        // do nothing by default
    }

    // =========================================================================
    // MakeDirectory
    // =========================================================================

    /**
     * This method is called to create a new directory in the file system emulated by the plugin.
     * <p/>
     * FAR calls exported function MakeDirectory, MakeDirectory calls this method
     * of the proper plugin instance object.
     *
     * @param opMode Combination of the {@link org.farmanager.api.jni.OperationModeFlags operation mode flags}
     *               This function should be ready to process OPM_SILENT flag.
     * @return If the function succeeds, the return value must be 1.
     *         If the function fails, 0 should be returned.
     *         If the function was interrupted by the user, it should return -1.
     */
    @UsedFromNativeCode
    public int makeDirectory (String directory, int opMode)
    {
        // fails by default
        return 0;
    }

    // =========================================================================
    // GetFiles
    // =========================================================================

    /**
     * This method is called to get files from the file system emulated by the plugin.
     * (FAR to plugin: "I want this file from your panel, destination is specified").
     * FAR calls exported function GetFiles, GetFiles calls this method
     * of the proper plugin instance object for every file.
     */
    @UsedFromNativeCode
    public int getFile (String fileName, String destPath, int move, int opmode)
    {
        return 0;
    }

    // =========================================================================
    // PutFiles
    // =========================================================================

    /**
     * This method is called to put file to the file system emulated by the plugin.
     * (FAR to plugin: "this file is for you, you should place it on your panel").
     * FAR calls exported function PutFiles, PutFiles calls this method
     * of the proper plugin instance object for every file.
     *
     * @return If the function succeeds, the return value must be 1 or 2.
     *         If the return value is 1, FAR tries to position the cursor
     *         to the most recently created file on the active panel.
     *         If the plugin returns 2, FAR does not perform any positioning operations.
     *         If the function fails, 0 should be returned.
     *         If the function was interrupted by the user, it should return -1.
     */
    @UsedFromNativeCode
    public int putFile (String fileName, int move, int opmode)
    {
        // fails by default
        return 0;
    }

    // =========================================================================
    // DeleteFiles
    // =========================================================================

    /**
     * This method is called to delete file in the file system emulated by the plugin.
     * (FAR to plugin: "this file from your panel needs to be deleted").
     * <p/>
     * FAR calls exported function DeleteFiles, DeleteFiles calls this method
     * of the proper plugin instance object for every file.
     *
     * @param opmode Operation mode.
     *               Can be a combination of {@link org.farmanager.api.jni.OperationModeFlags}
     * @return If the function succeeds, the return value must be TRUE.
     *         If the function fails, FALSE should be returned.
     */
    @UsedFromNativeCode
    public int deleteFile (String fileName, int opmode)
    {
        // method fails by default
        return 0;
    }

    // =========================================================================
    // ProcessKey
    // =========================================================================

    /**
     * Allows to override standard control keys processing in a plugin panel.
     * <p/>
     * FAR calls exported function ProcessKey, ProcessKey calls this method
     * of the proper plugin instance object.
     * <p/>
     * for keys F1-F12, you can use VK_F1..VK_F12 constants
     *
     * @return Return FALSE to use standard FAR key processing.
     *         If the plugin processes the key combination by itself, it should return TRUE
     */
    @UsedFromNativeCode
    public int processKey (int key, int controlState)
    {
        // standard FAR key processing by default
        return 0;
    }


    public int processEvent(int i1, int i2, int key, int controlState) {
        // do nothing by default
        return 0;
    }

}
