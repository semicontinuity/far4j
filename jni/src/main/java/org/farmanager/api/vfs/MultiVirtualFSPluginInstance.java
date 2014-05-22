package org.farmanager.api.vfs;

import org.apache.log4j.Logger;
import org.farmanager.api.AbstractPluginInstance;
import org.farmanager.api.PanelMode;
import org.farmanager.api.PluginPanelItem;
import org.farmanager.api.jni.FarInfoPanelLine;
import org.farmanager.api.jni.OpenPanelInfoFlags;
import org.farmanager.api.jni.UsedFromNativeCode;

/**
 * A plugin instance for a common case when there are many PanelContentProviders...
 * (one of them is 'current')
 * TODO: create high-level ('nodes') api!
 */
public abstract class MultiVirtualFSPluginInstance extends AbstractPluginInstance {

    private static final Logger LOGGER = Logger.getLogger(MultiVirtualFSPluginInstance.class);

    protected PanelContentProvider panelContentProvider;

    // =========================================================================

    @UsedFromNativeCode
    @Override
    public void init() {
        LOGGER.info("command line=" + commandLine);
        navigateToSessionList();
        if (commandLine != null)
            setDirectory(commandLine);
    }


    /**
     * Related to FAR exported function GetOpenPluginInfo
     *
     * @return Current plugin directory.
     *         If a plugin does not support a current directory, set it to an empty string.
     *         If a plugin returns an empty string in this field,
     * it will be closed automatically when Enter is pressed on "..".
     */
    @Override
    public String getCurDir() {
        final String currentDirectory = panelContentProvider.getCurrentDirectory();
        LOGGER.info("currentDirectory: " + currentDirectory);
        return currentDirectory;
    }


    /**
     * The SetDirectory exported function is called to set the current directory
     * in the file system emulated by the plugin.
     * SetDirectory exported function calls this method
     * for a specified plugin instance
     */
    @Override
    public void setDirectory(final String directory) {
        LOGGER.info("setDirectory " + directory);
        panelContentProvider.setDirectory(directory);
    }

    /**
     *
     * @param opMode Combination of the operation mode flags.
     * This function should be ready to process OPM_SILENT flag.

     */
    @Override
    public int makeDirectory(final String directory, final int opMode) {
        // TODO it is not clear when directory has value???
        return panelContentProvider.makeDirectory(directory, opMode);
    }

    // =========================================================================
    // GetFile
    // =========================================================================
    @Override
    public int getFile(String fileName, String destPath, int move, int opmode) {
        return panelContentProvider.getFile(fileName, destPath, move, opmode);
    }


    // =========================================================================
    // PutFile
    // =========================================================================
    @Override
    public int putFile (final String fileName, final int move, final int opmode) {
        return panelContentProvider.putFile(fileName, move, opmode);
    }

    // =========================================================================
    // DeleteFiles
    // =========================================================================
    @Override
    public int deleteFile(String fileName, int opmode) {
        return panelContentProvider.deleteFile(fileName, opmode);
    }


    /**
     * @param opMode a combination of OPM_* flags
     */
    @Override
    public PluginPanelItem[] getFindData(final int opMode) {
        LOGGER.info("getFindData " + opMode);
        final PluginPanelItem[] findData = panelContentProvider.getFindData(opMode);
        LOGGER.info("getFindData returns " + findData.length + " entries");
        return findData;
    }

    @Override
    public int getStartPanelMode() {
        return panelContentProvider.getStartPanelMode();
    }

    /**
     * Related to FAR exported function GetOpenPluginInfo
     *
     * @return Plugin panel title.
     */
    @Override
    public String getPanelTitle() {
        return panelContentProvider.getPanelTitle();
    }

    @Override
    public PanelMode[] getPanelModes() {
        return panelContentProvider.getPanelModes();
    }

    @Override
    public FarInfoPanelLine[] getInfoPanelLines () {
        return panelContentProvider.getInfoPanelLines ();
    }

    /**
     * Related to FAR exported function GetOpenPanelInfo
     *
     * @return Combination of the OPIF_* constants
     */
    @Override
    public long getFlags() {
//        LOGGER.debug("Add dots");
        return OpenPanelInfoFlags.OPIF_SHOWPRESERVECASE | OpenPanelInfoFlags.OPIF_ADDDOTS;
    }

    // =========================================================================


    @Override
    public int processKey(int key, int controlState) {
//        LOGGER.info("processKey " + Integer.toHexString(key) + " " + Integer.toHexString(controlState));
//        return panelContentProvider.processKey(key, controlState);
        return 0;
    }

    @Override
    public int processEvent(int i1, int i2, int key, int controlState) {
//        LOGGER.info("processEvent " + Integer.toHexString(i1) + ' ' + Integer.toHexString(i2) + ' ' + Integer.toHexString(key) + " " + Integer.toHexString(controlState));
        return panelContentProvider.processKey(key, controlState);
//        return 0;
    }

    public abstract void navigateToSessionList();
}
