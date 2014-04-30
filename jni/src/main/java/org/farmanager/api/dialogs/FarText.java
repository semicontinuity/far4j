package org.farmanager.api.dialogs;

import org.farmanager.api.jni.DialogItemType;
import org.farmanager.api.jni.FarDialogItemFlags;
import static org.farmanager.api.jni.FarDialogItemFlags.*;


public class FarText extends FarDialogItem {

    public int getType() {
        return DialogItemType.DI_TEXT.value();
    }

    // TODO?
    public void setBoxColor(final boolean flag) {
        flags |= DIF_BOXCOLOR.value();
    }

    // TODO: dup with FarButton
    public void setCenterGroup (final boolean flag) {
        setBooleanFlagByMask(flag, DIF_CENTERGROUP.value());
    }

    public void setSeparator (final boolean flag) {
        setBooleanFlagByMask(flag, DIF_SEPARATOR.value());
    }

    public void setSeparator2 (final boolean flag) {
        setBooleanFlagByMask(flag, DIF_SEPARATOR2.value());
    }

    // TODO: dup with FarBox
    public void setShowAmpersand (final boolean show) {
        setBooleanFlagByMask (show, FarDialogItemFlags.DIF_SHOWAMPERSAND.value());
    }

    public void setCenterText (final boolean center) {
        setBooleanFlagByMask (center, FarDialogItemFlags.DIF_CENTERTEXT.value());
    }

}
