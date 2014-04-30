package org.farmanager.api.ofs;

import org.farmanager.api.jni.PanelColumnType;
import org.farmanager.api.panels.NamedColumnDescriptor;

import java.lang.reflect.Method;

public class MethodMappedColumnDescriptor extends NamedColumnDescriptor
{
    private final Method method;

    public MethodMappedColumnDescriptor (
        Method method,
        String title,
        PanelColumnType type,
        int width)
    {
        super (title, type, width);
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }
}
