package org.farmanager.api.dialogs;

import org.farmanager.api.jni.DialogItemType;
import static org.farmanager.api.jni.FarDialogItemFlags.*;

/**
 * Corresponds to a FAR ListBox control
 */
public class FarButton extends FarDialogItem {

    public int getType() {
        return DialogItemType.DI_BUTTON.value();
    }

    public void setNoClose (final boolean flag) {
        setBooleanFlagByMask(flag, ~DIF_BTNNOCLOSE.value());
    }

    public void setCenterGroup (final boolean flag) {
        setBooleanFlagByMask(flag, ~DIF_CENTERGROUP.value());
    }

    public void setNoBrackets (final boolean flag) {
        setBooleanFlagByMask(flag, ~DIF_NOBRACKETS.value());
    }
}
