package org.farmanager.api.jni;

/**
 * Dialog Item type
 * Corresponds to DI_* constants in FAR
 */
@SuppressWarnings({"ClassWithTooManyFields"})
public enum DialogItemType {

    DI_TEXT(0),
    DI_VTEXT(1),
    DI_SINGLEBOX(2),
    DI_DOUBLEBOX(3),
    DI_EDIT(4),
    DI_PSWEDIT(5),
    DI_FIXEDIT(6),
    DI_BUTTON(7),
    DI_CHECKBOX(8),
    DI_RADIOBUTTON(9),
    DI_COMBOBOX(10),
    DI_LISTBOX(11),
    DI_USERCONTROL(255);

    int value;

    public int value() {
        return value;
    }

    DialogItemType(int value) {
        this.value = value;
    }
}
