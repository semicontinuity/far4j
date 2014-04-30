package org.farmanager.api.dialogs;

import static org.farmanager.api.jni.FarDialogItemFlags.*;

/**
 * A base class for FAR dialog items (UI widgets)
 */
public abstract class FarDialogItem extends FarComponent
{
    public boolean focused;
    public boolean selected;
    /**
     * @see org.farmanager.api.jni.FarDialogItemFlags
     */
    public int flags;
    public String data;

    /**
     * Get a type of the dialog item
     *
     * @return a type code of dialog item, one of DI_* constants for FAR API.
     * @see org.farmanager.api.jni.DialogItemType
     */
    public abstract int getType();

    /**
     * @see org.farmanager.api.jni.FarDialogItemFlags
     */
    public int getFlags()
    {
        return flags;
    }

    public boolean isFocused()
    {
        return focused;
    }

    public void setFocused(boolean focused)
    {
        this.focused = focused;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }


    public void setDisabled(final boolean disabled)
    {
        setBooleanFlagByMask(disabled, DIF_DISABLE.value());
    }

    /**
     * Not applicable to all controls?
     */
    public void setFocusable(final boolean focusable)
    {
        setBooleanFlagByMask(focusable, ~DIF_NOFOCUS.value());
    }

    /**
     * Not applicable to all controls?
     */
    public void setColor(final byte color)
    {
        flags |= (DIF_SETCOLOR.value() | color);
    }


    protected void setBooleanFlagByMask(boolean flag, final int mask)
    {
        if (flag)
            flags |= mask;
        else
            flags &= mask;
    }

    public String getData()
    {
        return data;
    }
}
