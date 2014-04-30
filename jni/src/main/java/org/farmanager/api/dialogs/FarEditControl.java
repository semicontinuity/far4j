package org.farmanager.api.dialogs;

import static org.farmanager.api.jni.DialogItemType.DI_EDIT;
import static org.farmanager.api.jni.FarDialogItemFlags.DIF_EDITEXPAND;
import static org.farmanager.api.jni.FarDialogItemFlags.DIF_VAREDIT;

public class FarEditControl extends FarAbstractEditControl
{
    public int getType() {
        return DI_EDIT.value();
    }

    public void setEditor  (final boolean edit){
        // Corresponds to DIF_EDITOR
        // TODO: not implemented
    }

    public void setExpandEnvironmentVariables (final boolean expand) {
        setBooleanFlagByMask(expand, DIF_EDITEXPAND.value());
    }

    /**
     * Usage to be clarified
     */
    public void setVarEdit (final boolean varEdit) {
        setBooleanFlagByMask(varEdit, DIF_VAREDIT.value());
    }
}
