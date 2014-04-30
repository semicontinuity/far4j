package org.farmanager.api.ofs;

import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class IntrospectionHelper
{
    public static final Logger LOGGER = Logger.getLogger (IntrospectionHelper.class);

    public static Object getProperty (final Object object, final String propertyName) throws
        NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        if (object == null) throw new IllegalArgumentException ();
        Method method = object.getClass ().getMethod (getterName (propertyName));
        return method.invoke (object);
    }

    public static String getterName (final String name)
    {
        if (name == null || name.length () == 0) throw new IllegalArgumentException ();
        return "get" + capitalize (name);
    }

    protected static String capitalize (final String name)
    {
        if (name == null || name.length () == 0)
        {
            return name;
        }
        return name.substring (0, 1).toUpperCase () + name.substring (1);
    }



    static Method findChildrenGetterMethod (Class<? extends Object> clazz) {
        final Method[] methods = clazz.getMethods ();
        for (Method method : methods)
        {
            NodeContentProvider.LOGGER.debug (method);
            final ChildrenGetter ann = method.getAnnotation (ChildrenGetter.class);
            NodeContentProvider.LOGGER.debug (ann);
            if (ann != null) return method;
        }
        return null;
    }
}
