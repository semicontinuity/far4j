package org.farmanager.api.jni;

/**
 * Open panel info flags.
 * Correspond to OPIF_* constants in FAR (OPENPANELINFO_FLAGS).
 */
@SuppressWarnings({"ClassWithTooManyFields"})
public interface OpenPanelInfoFlags {
    /**
     * Use filters in the plugin panel.
     */
    long OPIF_DISABLEFILTER         = 0x0000000000000001L;
    /**
     * Use sort groups in the plugin panel.
     */
    long OPIF_DISABLESORTGROUPS     = 0x0000000000000002L;
    /**
     * Use file highlighting in the plugin panel.
     */
    long OPIF_DISABLEHIGHLIGHTING   = 0x0000000000000004L;
    /**
     * Add two dots (..) entry, if absent, automatically.
     */
    long OPIF_ADDDOTS               = 0x0000000000000008L;
    /**
     * Select folders regardless of FAR configuration settings.
     */
    long OPIF_RAWSELECTION          = 0x0000000000000010L;
    /**
     * Enables to use standard FAR file processing if a required file operation is not implemented in plugin.
     * If this flag is specified, items in the plugin panel must be real file names.
     */
    long OPIF_REALNAMES             = 0x0000000000000020L;
    /**
     * Show by default names without path in all view modes.
     */
    long OPIF_SHOWNAMESONLY         = 0x0000000000000040L;
    /**
     * Show right aligned names in all view modes by default.
     */
    long OPIF_SHOWRIGHTALIGNNAMES   = 0x0000000000000080L;
    /**
     * Show the file names in original case regardless of FAR configuration settings.
     */
    long OPIF_SHOWPRESERVECASE      = 0x0000000000000100L;
    /**
     * Convert file times to FAT format when performing Compare folders command.
     * Set this flag if the plugin file system does not provide the precision required for standard time comparing.
     */
    long OPIF_COMPAREFATTIME        = 0x0000000000000400L;
    long OPIF_EXTERNALGET           = 0x0000000000000800L;
    long OPIF_EXTERNALPUT           = 0x0000000000001000L;
    long OPIF_EXTERNALDELETE        = 0x0000000000002000L;
    /**
     * Can be used only with OPIF_REALNAMES.
     * Forces usage of corresponding Far functions even if required function is exported by plugin.
     */
    long OPIF_EXTERNALMKDIR         = 0x0000000000004000L;
    /**
     * Use only highlighting by attributes in plugin panel.
     * Only the groups for which the flag "[ ] Match file mask(s)" is not set will be analyzed.
     */
    long OPIF_USEATTRHIGHLIGHTING   = 0x0000000000008000L;
    /**
     * Plugin fills CRC32 field.
     */
    long OPIF_USECRC32              = 0x0000000000010000L;
    long OPIF_USEFREESIZE           = 0x0000000000020000L;
    long OPIF_SHORTCUT              = 0x0000000000040000L;
    long OPIF_NONE                  = 0;
}
