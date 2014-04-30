package org.farmanager.api.ofs;

import org.farmanager.api.panels.NamedColumnDescriptor;
import org.farmanager.api.jni.PanelColumnType;

public class PropertyMappedColumnDescriptor extends NamedColumnDescriptor
{
    private final String property;

    public PropertyMappedColumnDescriptor (
        String property,
        String title, PanelColumnType type, int width)
    {
        super (title, type, width);
        this.property = property;
    }

    public String getProperty ()
    {
        return property;
    }
}
