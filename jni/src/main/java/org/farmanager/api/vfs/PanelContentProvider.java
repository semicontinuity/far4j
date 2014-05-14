package org.farmanager.api.vfs;

import org.farmanager.api.PanelMode;
import org.farmanager.api.PluginPanelItem;
import org.farmanager.api.jni.FarInfoPanelLine;

/**
 * A panel content provider.
 * This is a java counterpart of FAR Panel API.
 * TODO: create high-level ("Nodes") API - see "hard" plugin
 */
public interface PanelContentProvider {

    PanelMode[] getPanelModes();

    int getStartPanelMode();

    String getPanelTitle();

    /**
     * @return A list of panel items
     * @param opMode see FAR docs TODO
     */
    PluginPanelItem[] getFindData (final int opMode);

    int getFile (String fileName, String destPath, int move, int opmode);

    int putFile(String fileName, int move, int opmode);

    int processKey(int key, int controlState);

    void setDirectory(String directory);

    /**
     * @return the "current directory" which is shown in the panel.
     * Note that the string returned must be the same as id column (file name) of the entry
     * in the parent "directory" before navigation to this directory.
     * FAR will automatically position the cursor on this item after navigation back to parent (..)
     * If in the parent there will be no "file" with this name, the cursor will be positioned on the first entry!
     */
    String getCurrentDirectory();

    int makeDirectory(String directory, int opMode);

    int deleteFile(String fileName, int opmode);

    FarInfoPanelLine[] getInfoPanelLines ();
}
