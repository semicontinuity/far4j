package org.farmanager.api.dialogs;

import org.farmanager.api.jni.FarDialogItemFlags;

public abstract class FarBox extends FarDialogItem {
    public void setLeftText (final boolean flag) {
        setBooleanFlagByMask (flag, FarDialogItemFlags.DIF_LEFTTEXT.value());
    }

    public void setShowAmpersand (final boolean show) {
        setBooleanFlagByMask (show, FarDialogItemFlags.DIF_SHOWAMPERSAND.value());
    }
}
