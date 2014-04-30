package org.farmanager.api;

import org.farmanager.api.jni.UsedFromNativeCode;

/**
 * An item in the panel
 * Corresponds to FAR's PluginPanelItem structure
 * TODO: All low-level code should be moved to jni package!
 */
@SuppressWarnings({"ClassWithTooManyFields"})
@UsedFromNativeCode
public class PluginPanelItem
{
    // FindData
    // The FindData structure contains a number of file parameters.
    // Read WIN32_FIND_DATA structure description in Windows programming manuals for details.
    // -------------------------------------------------------------------------

    /**
     * Corresponds to DWORD FindData.dwFileAttributes
     * Can be the combination of FILE_ATTRIBUTE_* constants
     */
    @UsedFromNativeCode
    public int     dwFileAttributes;

    // -------------------------------------------------------------------------
    // Related to DWORD FindData.dwFileAttributes
    // -------------------------------------------------------------------------
    public static final int FILE_ATTRIBUTE_READONLY             = 0x00000001;
    public static final int FILE_ATTRIBUTE_HIDDEN               = 0x00000002;
    public static final int FILE_ATTRIBUTE_SYSTEM               = 0x00000004;
    public static final int FILE_ATTRIBUTE_DIRECTORY            = 0x00000010;
    public static final int FILE_ATTRIBUTE_ARCHIVE              = 0x00000020;
    public static final int FILE_ATTRIBUTE_ENCRYPTED            = 0x00000040;
    public static final int FILE_ATTRIBUTE_NORMAL               = 0x00000080;
    public static final int FILE_ATTRIBUTE_TEMPORARY            = 0x00000100;
    public static final int FILE_ATTRIBUTE_SPARSE_FILE          = 0x00000200;
    public static final int FILE_ATTRIBUTE_REPARSE_POINT        = 0x00000400;   // actually, a symlink
    public static final int FILE_ATTRIBUTE_COMPRESSED           = 0x00000800;
    public static final int FILE_ATTRIBUTE_OFFLINE              = 0x00001000;
    public static final int FILE_ATTRIBUTE_NOT_CONTENT_INDEXED  = 0x00002000;
    // -------------------------------------------------------------------------


    // FILETIME ftCreationTime;
    @UsedFromNativeCode
    public long lCreationTime;
    // FILETIME ftLastAccessTime;
    @UsedFromNativeCode
    public long lLastAccessTime;
    // FILETIME ftLastWriteTime;
    @UsedFromNativeCode
    public long lLastWriteTime;

    /**
     * Corresponds to FindData.nFileSizeHigh
     */
    @UsedFromNativeCode
    public int     nFileSizeHigh;

    /**
     * Corresponds to FindData.nFileSizeLow
     */
    @UsedFromNativeCode
    public int     nFileSizeLow;

    // DWORD dwReserved0 - ignored
    // DWORD dwReserved1 - ignored

    /**
     * Corresponds to TCHAR FindData.cFileName[MAX_PATH]
     */
    @UsedFromNativeCode
    public String  cFileName;

    //TCHAR cAlternateFileName[14]; // TODO not used yet
    // -------------------------------------------------------------------------


    // DWORD PackSizeHigh
    // Specifies the high-order DWORD value of the packed file size, in bytes. Currently not used.

    // DWORD PackSize;
    // Specifies the low-order DWORD value of the packed file size, in bytes.

    /**
     * Flags
     * Can be a combination of the PPIF_* constants
     */
    @UsedFromNativeCode
    public int flags;
    // -------------------------------------------------------------------------
    /**
     * Use internal FAR description processing.
     * This flag can be set for processed files in DeleteFiles, GetFiles and PutFiles functions.
     * In that case FAR will update description files with names returned in the GetOpenPluginInfo function.
     */
    public static final int PPIF_PROCESSDESCR   = 0x80000000;
    /**
     * In Control functions FCTL_GETPANELINFO, FCTL_GETANOTHERPANELINFO, FCTL_SETSELECTION and FCTL_SETANOTHERSELECTION
     * this flag allows to check and set items selection.
     * In PutFiles, GetFiles and ProcessHostFile functions, if an operation has failed,
     * but part of files was successfully processed, the plugin can remove selection only from the processed files.
     * To perform it, the plugin should clear the PPIF_SELECTED flag in processed items in the PluginPanelItem list
     * passed to the function.
     */
    public static final int PPIF_SELECTED       = 0x40000000;
    /**
     * If this flag is set, FAR considers the UserData field a pointer to a user data structure.
     * Read the description of the UserData field for more information.
     */
    public static final int PPIF_USERDATA       = 0x20000000;
    // -------------------------------------------------------------------------



    /**
     * Corresponds to DWORD NumberOfLinks
     * Number of hard links.
     */
    @UsedFromNativeCode
    public int numberOfLinks;

    /**
     * Corresponds to char *Description
     * Points to a file description.
     * Plugins can use this field to pass file descriptions to FAR.
     * If you do not need to specify descriptions, set Description to NULL.
     * If a plugin uses standard FAR description processing
     * and has passed description file names to FAR in the GetOpenPluginInfo function, this field also must be NULL.
     */
    @UsedFromNativeCode
    public String description;

    /**
     * Corresponds to char *Owner
     * Points to a file owner name.
     * Plugins can use this field to pass file owner names to FAR.
     * If you do not need it, set this field to NULL.
     */
    @UsedFromNativeCode
    public String owner;

    /**
     * Corresponds to char **CustomColumnData, int CustomColumnNumber
     * "Points to an array of string addresses for plugin defined column types.
     * The first string contains data for C0 column type, the second - for C1 and so on.
     * Up to 10 additional column types from C0 to C9 can be defined.
     * If you do not need additional column types, set this field to NULL."
     * "Number of data strings for additional column types."
     */
    @UsedFromNativeCode
    public String[] customColumns;

    /**
     * Corresponds to DWORD UserData
     * "This field can be used by the plugin to store either a 32-bit value or a pointer to a data structure.
     * In the latter case, when UserData points to a data structure,
     * the first field of this structure must be a 32-bit value equal to the structure size
     * and the plugin must set PPIF_USERDATA in the Flags field.
     * It allows FAR to copy the structure correctly to FAR internal buffers
     * and later pass it to the  plugin in PluginPanelItem lists.
     * In FreeFindData function the plugin must free the memory occupied by this additional structure."
     * TODO: just declared, not used
     */
    @UsedFromNativeCode
    public int userData;

    /**
     * Corresponds to DWORD CRC32
     * "A 32-bit CRC (checksum) value. FAR does not use this field."
     */
    @UsedFromNativeCode
    public int crc32;


    /**
     * Converts java date (as returned by System.currentTimeMillis) to a long that corresponds to WIN32 FILETIME
     */
    @SuppressWarnings({"MagicNumber"})
    public static long dateToFILETIME(final long date)
    {
        return 10000*(date + 11644473600000L);
    }
}
