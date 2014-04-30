package org.farmanager.api.dialogs;

import static org.farmanager.api.jni.DialogItemType.DI_COMBOBOX;
import static org.farmanager.api.jni.FarDialogItemFlags.*;

public class FarComboBox extends FarListControl {

    public int getType() {
        return DI_COMBOBOX.value();
    }

    public void setDropDown(final boolean dropDown) {
        setBooleanFlagByMask(dropDown, DIF_DROPDOWNLIST.value());
    }

    public void setExpandEnvironmentVariables (final boolean expand) {
        setBooleanFlagByMask(expand, DIF_EDITEXPAND.value());
    }

    public void setReadonly (final boolean readonly) {
        setBooleanFlagByMask(readonly, DIF_READONLY.value());
    }

    public void setSelectOnEntry (final boolean selectOnEntry) {
        setBooleanFlagByMask(selectOnEntry, DIF_SELECTONENTRY.value());
    }

    /**
     * Usage to be clarified
     */
    public void setVarEdit (final boolean varEdit) {
        setBooleanFlagByMask(varEdit, DIF_VAREDIT.value());
    }
}
