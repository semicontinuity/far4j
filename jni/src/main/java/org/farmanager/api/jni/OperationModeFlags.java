package org.farmanager.api.jni;

/**
 * Operation mode flags
 * Correspond to OPM_* constants in FAR
 * The values are flags that can be combined in opMode parameter
 * passed to exported functions GetFindData, DeleteFile
 */
public enum OperationModeFlags
{
    /**
     * Plugin should minimize user requests if possible,
     * because the called function is only a part of a more complex file operation.
     */
    OPM_SILENT (0x0001),

    /**
     * Plugin function is called from Find file or another directory scanning command.
     * Screen output has to be minimized.
     */
    OPM_FIND (0x0002),

    /**
     * Plugin function is called as part of a file view operation.
     */
    OPM_VIEW (0x0004),

    /**
     * Plugin function is called as part of a file edit operation.
     */
    OPM_EDIT (0x0008),

    /**
     * Plugin function is called to get or put file with file descriptions.
     */
    OPM_DESCR (0x0020),

    /**
     * All files in host file of file based plugin should be processed.
     * This flag is set when executing Shift-F2 and Shift-F3 FAR commands
     * outside of host file. Passed to plugin functions files list also contains
     * all necessary information, so plugin can either ignore this flag
     * or use it to speed up processing.
     */
    OPM_TOPLEVEL (0x0010);


    int value;

    public int value ()
    {
        return value;
    }

    OperationModeFlags (int value)
    {
        this.value = value;
    }
}
