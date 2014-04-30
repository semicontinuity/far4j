package org.farmanager.api.ofs;

public @interface Mode {
    int name();
    int width() default 0;
}
