package org.farmanager.api;

/**
 * Not suitable for getStartPanelMode
 * TODO: All low-level code (containing FAR constants) should be moved to jni package!
 */
public interface PanelModeId
{
    int BRIEF = 0;
    int MEDIUM = 1;
    int FULL = 2;
    int WIDE = 3;
    int DETAILED = 4;
    int DESCRIPTIONS = 5;
    int LONG_DESCRIPTIONS = 6;
    int FILE_OWNERS = 7;
    int FILE_LINKS = 8;
    int ALTERNATIVE_FULL = 9;
}
