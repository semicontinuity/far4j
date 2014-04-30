package org.farmanager.api.jni;

/**
 * A collection of constants, corresponding to LIF_* constants in FAR API.
 * @author Igor A. Karpov (ikar)
 */
public enum ListItemFlags
{
    ;
    public static final int LIF_SELECTED            = 0x00010000;
    public static final int LIF_CHECKED             = 0x00020000;
    public static final int LIF_SEPARATOR           = 0x00040000;
    public static final int LIF_DISABLE             = 0x00080000;
    public static final int LIF_DELETEUSERDATA      = 0x80000000;
}
