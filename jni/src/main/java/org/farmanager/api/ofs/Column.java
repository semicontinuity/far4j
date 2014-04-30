package org.farmanager.api.ofs;

import org.farmanager.api.jni.PanelColumnType;

/** @author Igor A. Karpov (ikar) */
public @interface Column
{
    PanelColumnType type();
    String title();
    Mode[] modes();
}
