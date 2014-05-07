package org.farmanager.plugins.jdbc;

import org.farmanager.api.dialogs.FarEditControl;

public class JDBCEditControl extends FarEditControl implements org.farmanager.plugins.jdbc.EditedValueProvider {
    public String getEditedValue() {
        return getData();
    }
}
