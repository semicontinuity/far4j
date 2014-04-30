package org.farmanager.api.jni;

public enum ProcessKeyFlags
{
    ;
    public static final int PKF_CONTROL = 1;
    public static final int PKF_ALT = 2;
    public static final int PKF_SHIFT = 4;
    public static final int PKF_PREPROCESS = 0x80000;

    // Convenience methods for keyboard handling

    public static boolean alt (int controlState)
    {
        return (controlState & PKF_ALT) == PKF_ALT;
    }

    public static boolean control (int controlState)
    {
        return (controlState & PKF_CONTROL) == PKF_CONTROL;
    }

    public static boolean shift (int controlState)
    {
        return (controlState & PKF_SHIFT) == PKF_SHIFT;
    }

    public static boolean noFlags (int controlState)
    {
        return ((controlState & (PKF_SHIFT|PKF_ALT|PKF_CONTROL)) == 0);
    }

    public static int clearedPreprocess (int key)
    {
        return key & (~PKF_PREPROCESS);
    }
}
