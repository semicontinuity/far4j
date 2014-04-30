package org.farmanager.api.panels;

import org.farmanager.api.jni.PanelColumnType;

/**
 * Used in high-level API for panel modes
 */
public class ColumnDescriptor {

    private int width;
    private PanelColumnType type;

    public ColumnDescriptor(PanelColumnType type, int width) {
        this.type = type;
        this.width = width;
    }

    public PanelColumnType getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }
}
