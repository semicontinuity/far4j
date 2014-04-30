package org.farmanager.api.dialogs;

import static org.farmanager.api.jni.FarDialogItemFlags.*;
import static org.farmanager.api.jni.DialogItemType.*;

/**
 * Corresponds to a FAR ListBox control
 */
public class FarListBox extends FarListControl {

    public int getType() {
        return DI_LISTBOX.value();
    }

    public void setFrame(final boolean frame) {
        setBooleanFlagByMask(frame, ~DIF_LISTNOBOX.value());
    }
}
