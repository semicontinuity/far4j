package org.farmanager.api.panels;

import org.farmanager.api.jni.PanelColumnType;

/**
 * Used in high-level API for panel modes
 */
public class NamedColumnDescriptor extends ColumnDescriptor {

    private String title;

    /**
     * @param width 0=automatic
     */
    public NamedColumnDescriptor(String title, PanelColumnType type, int width) {
        super(type, width);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
