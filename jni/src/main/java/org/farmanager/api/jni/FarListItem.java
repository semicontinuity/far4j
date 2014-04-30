package org.farmanager.api.jni;

/**
 * @author Igor A. Karpov (ikar)
 */
@UsedFromNativeCode
public class FarListItem
{
    /**
     * Corresponds to Flags field in FAR API structure FarListItem
     * Can be a combination of the constants from
     * {@linkplain org.farmanager.api.jni.ListItemFlags}
     */
    @UsedFromNativeCode
    public int flags;

    /**
     * Item text.
     * TODO: FAR API declares this field as char[128]. Check the length!
     */
    @UsedFromNativeCode
    public String text;


    public FarListItem (int flags, String text)
    {
        this.flags = flags;
        this.text = text;
    }
}
