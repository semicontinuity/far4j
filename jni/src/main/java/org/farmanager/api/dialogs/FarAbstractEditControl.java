package org.farmanager.api.dialogs;

import static org.farmanager.api.jni.FarDialogItemFlags.DIF_READONLY;
import static org.farmanager.api.jni.FarDialogItemFlags.DIF_SELECTONENTRY;

public abstract class FarAbstractEditControl extends FarDialogItem
{

    public void setHistory  (final boolean edit){
        // Corresponds to DIF_HISTORY
        // TODO: not implemented
    }

    public void setManualAddHistory  (final boolean edit){
        // Corresponds to DIF_HISTORY
        // TODO: not implemented
    }

    public void setUseLastHistory  (final boolean edit){
        // Corresponds to DIF_MANUALADDHISTORY
        // TODO: not implemented
    }

    public void setReadOnly (final boolean readOnly) {
        setBooleanFlagByMask(readOnly, DIF_READONLY.value());
    }

    public void setSelectOnEntry (final boolean selectOnEntry) {
        setBooleanFlagByMask(selectOnEntry, DIF_SELECTONENTRY.value());
    }
}
