package org.farmanager.api.dialogs;

import static org.farmanager.api.jni.DialogItemType.DI_DOUBLEBOX;


public class FarDoubleBox extends FarBox {

    public int getType() {
        return DI_DOUBLEBOX.value();
    }
}
