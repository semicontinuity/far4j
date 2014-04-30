package org.farmanager.api;

import org.farmanager.api.jni.UsedFromNativeCode;

/**
 * A low-level structure for interfacing FAR Dialog API, representing a dialog item.
 * Passed to native code.
 * TODO: Pass FarDialogItems to Dialog function, remove this class?
 * TODO: All low-level code (native wrappers) should be moved to jni package!
 */
@SuppressWarnings({"ClassWithTooManyFields"})
@UsedFromNativeCode
public class InitDialogItem {

    @UsedFromNativeCode
    public int type;

    @UsedFromNativeCode
    public int x1;

    @UsedFromNativeCode
    public int y1;

    @UsedFromNativeCode
    public int x2;

    @UsedFromNativeCode
    public int y2;

    @UsedFromNativeCode
    public int focus;

    @UsedFromNativeCode
    public int selected;

    @UsedFromNativeCode
    public int flags;

    @UsedFromNativeCode
    public int defaultButton;

    /**
     * Corresponds to Data.Data
     */
    @UsedFromNativeCode
    public String data;

    @UsedFromNativeCode
    public int intReturnValue;

    /**
     * For listboxes and comboboxes, points to array of FarListItem objects.
     */
    @UsedFromNativeCode
    public Object param;


    public InitDialogItem(
        int type,
        int x1,
        int y1,
        int x2,
        int y2,
        int focus,
        int selected,
        int flags,
        int defaultButton,
        String data)
    {
        this.type = type;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;        
        this.focus = focus;
        this.selected = selected;
        this.flags = flags;
        this.defaultButton = defaultButton;
        this.data = data;
    }
}