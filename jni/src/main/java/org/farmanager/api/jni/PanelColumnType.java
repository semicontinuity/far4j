package org.farmanager.api.jni;

@SuppressWarnings({"ClassWithTooManyFields"})
public enum PanelColumnType {

    ID("N"),
    DESCRIPTION("Z"),
    OWNER("O"),
    CREATION_DATE("DC"),
    CUSTOM0("C0"),
    CUSTOM1("C1"),
    CUSTOM2("C2"),
    CUSTOM3("C3"),
    CUSTOM4("C4"),
    CUSTOM5("C5"),
    CUSTOM6("C6"),
    CUSTOM7("C7"),
    CUSTOM8("C8"),
    CUSTOM9("C9");


    String value;

    public String value() {
        return value;
    }

    PanelColumnType(String value) {
        this.value = value;
    }
}
