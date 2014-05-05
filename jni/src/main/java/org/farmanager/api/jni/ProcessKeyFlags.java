package org.farmanager.api.jni;

public enum ProcessKeyFlags
{
    ;
    // Copied from KEY_EVENT_RECORD structure (dwControlKeyState)
    public static final int RIGHT_ALT_PRESSED   = 0x0001;
    public static final int LEFT_ALT_PRESSED    = 0x0002;
    public static final int RIGHT_CTRL_PRESSED  = 0x0004;
    public static final int LEFT_CTRL_PRESSED   = 0x0008;
    public static final int SHIFT_PRESSED       = 0x0010;

//    public static final int PKF_PREPROCESS = 0x80000;

    // Convenience methods for keyboard handling

    public static boolean alt (int controlState)
    {
        return (controlState & (RIGHT_ALT_PRESSED|LEFT_ALT_PRESSED)) != 0;
    }

    public static boolean control (int controlState)
    {
        return (controlState & (RIGHT_CTRL_PRESSED|LEFT_CTRL_PRESSED)) != 0;
    }

    public static boolean shift (int controlState)
    {
        return (controlState & SHIFT_PRESSED) != 0;
    }

    public static boolean noFlags (int controlState)
    {
        return ((controlState & (RIGHT_ALT_PRESSED|LEFT_ALT_PRESSED|RIGHT_CTRL_PRESSED|LEFT_CTRL_PRESSED|SHIFT_PRESSED)) == 0);
    }

    public static int clearedPreprocess (int key)
    {
        return key; /*& (~PKF_PREPROCESS);*/
    }
}
