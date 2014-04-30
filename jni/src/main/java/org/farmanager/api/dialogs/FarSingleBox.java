package org.farmanager.api.dialogs;

import org.farmanager.api.jni.DialogItemType;

public class FarSingleBox extends FarBox {

    public int getType() {
        return DialogItemType.DI_SINGLEBOX.value();
    }
}
