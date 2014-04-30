package org.farmanager.api.vfs;

import org.farmanager.api.PanelMode;
import org.farmanager.api.PluginPanelItem;
import org.farmanager.api.jni.FarInfoPanelLine;

/**
 * A convenience class to implement PanelContentProviders
 * TODO replace by high-level ("nodes") API
 */
public class AbstractPanelContentProvider implements PanelContentProvider
{

    public PanelMode[] getPanelModes() {
        return null;
    }

    /**
     * @return start panel mode, 0 means "FAR default"
     */
    public int getStartPanelMode() {
        return 0;
    }

    /**
     * @return panel title (nothing by default)
     */
    public String getPanelTitle() {
        return null;
    }

    public PluginPanelItem[] getFindData(final int opMode) {
        // no panel data by default
        return new PluginPanelItem[0];  // TODO: null?
    }

    public int getFile(String fileName, String destPath, int move, int opmode) {
        // do nothing by default
        return 0;
    }

    public int processKey(int key, int controlState) {
        // do nothing by default
        return 0;
    }

    public void setDirectory(String directory) {
        // do nothing by default
    }

    public String getCurrentDirectory() {
        // no current directory info by default
        return null;
    }

    public int makeDirectory(String directory, int opMode) {
        // do nothing by default
        return 0;
    }

    public int deleteFile(String fileName, int opmode) {
        // do nothing by default
        return 0;
    }

    public FarInfoPanelLine[] getInfoPanelLines ()
    {
        return null;
    }
}
