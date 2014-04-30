package org.farmanager.api.jni;

/**
 * Open plugin info flags
 * Correspond to OPIF_* constants in FAR
 */
@SuppressWarnings({"ClassWithTooManyFields"})
public enum OpenPluginInfoFlags
{
    /**
     * Use filters in the plugin panel.
     */
    OPIF_USEFILTER (0x0001),
    /**
     * Use sort groups in the plugin panel.
     */
    OPIF_USESORTGROUPS (0x0002),
    /**
     * Use file highlighting in the plugin panel.
     */
    OPIF_USEHIGHLIGHTING (0x0004),
    /**
     * Add two dots (..) entry, if absent, automatically.
     */
    OPIF_ADDDOTS (0x0008),
    /**
     * Select folders regardless of FAR configuration settings.
     */
    OPIF_RAWSELECTION (0x0010),
    /**
     * Enables to use standard FAR file processing if a required file operation is not implemented in plugin.
     * If this flag is specified, items in the plugin panel must be real file names.
     */
    OPIF_REALNAMES (0x0020),
    /**
     * Show by default names without path in all view modes.
     */
    OPIF_SHOWNAMESONLY (0x0040),
    /**
     * Show right aligned names in all view modes by default.
     */
    OPIF_SHOWRIGHTALIGNNAMES (0x0080),
    /**
     * Show the file names in original case regardless of FAR configuration settings.
     */
    OPIF_SHOWPRESERVECASE (0x0100),
    OPIF_FINDFOLDERS (0x0200),
    /**
     * Convert file times to FAT format when performing Compare folders command.
     * Set this flag if the plugin file system does not provide the precision required for standard time comparing.
     */
    OPIF_COMPAREFATTIME (0x0400),
    OPIF_EXTERNALGET (0x0800),
    OPIF_EXTERNALPUT (0x1000),
    OPIF_EXTERNALDELETE (0x2000),
    /**
     * Can be used only with OPIF_REALNAMES.
     * Forces usage of corresponding Far functions even if required function is exported by plugin.
     */
    OPIF_EXTERNALMKDIR (0x4000),
    /**
     * Use only highlighting by attributes in plugin panel.
     * Only the groups for which the flag "[ ] Match file mask(s)" is not set will be analyzed.
     */
    OPIF_USEATTRHIGHLIGHTING (0x8000);


    int value;

    public int value ()
    {
        return value;
    }

    OpenPluginInfoFlags (int value)
    {
        this.value = value;
    }
}
