package org.farmanager.api.dialogs;

import static org.farmanager.api.jni.FarDialogItemFlags.*;
import org.farmanager.api.jni.FarListItem;

/**
 * A base class for FAR list controls: FarComboBox and FarListBox
 */
public abstract class FarListControl extends FarDialogItem {

    public int selectedIndex = -1;
    public FarListItem[] items;

    public void setAutoHighlight(boolean autoHighlight) {
        setBooleanFlagByMask (autoHighlight, DIF_LISTAUTOHIGHLIGHT.value());
    }

    public void setDisplayHotKeys(boolean displayHotKeys) {
        setBooleanFlagByMask (displayHotKeys, DIF_LISTNOAMPERSAND.value());
    }

    public void setWrapMode (boolean wrapMode) {
        setBooleanFlagByMask(wrapMode, DIF_LISTWRAPMODE.value());
    }
}
