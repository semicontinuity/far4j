package org.farmanager.api;

import org.farmanager.api.jni.UsedFromNativeCode;
import org.farmanager.api.panels.ColumnDescriptor;
import org.farmanager.api.panels.NamedColumnDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Corresponds to FAR panel mode
 * Passed to native code
 * TODO: All low-level code (native wrappers) should be moved to jni package!
 */
@SuppressWarnings({"ClassWithTooManyFields"})
@UsedFromNativeCode
public class PanelMode
{

    private List<NamedColumnDescriptor> columns = new ArrayList<NamedColumnDescriptor> ();
    private List<ColumnDescriptor> statusColumns = new ArrayList<ColumnDescriptor> ();

    // ==================================================================================
    // High level, object-oriented API
    // ==================================================================================

    public PanelMode (
        final NamedColumnDescriptor[] columns,
        final boolean fullScreen,
        final ColumnDescriptor[] statusColumns)
    {
        this.fullScreen = fullScreen ? 1 : 0;
        setColumnDescriptors (columns);
        setStatusColumnDescriptors (statusColumns);
        initColumnFields ();
    }


    public void setFullScreen (int fullScreen)
    {
        this.fullScreen = fullScreen;
    }

    public void setDetailedStatus (int detailedStatus)
    {
        this.detailedStatus = detailedStatus;
    }

    public void setAlignExtensions (int alignExtensions)
    {
        this.alignExtensions = alignExtensions;
    }

    public void setCaseConversion (int caseConversion)
    {
        this.caseConversion = caseConversion;
    }


    public void setColumnDescriptors (final NamedColumnDescriptor[] descriptors)
    {
        columns.clear ();
        if (descriptors == null) return;
        for (NamedColumnDescriptor columnDescriptor : descriptors)
        {
            columns.add (columnDescriptor);
        }
    }

    public NamedColumnDescriptor getColumnDescriptor (final int index)
    {
        return columns.get (index);
    }


    public void setStatusColumnDescriptors (final ColumnDescriptor[] descriptors)
    {
        statusColumns.clear ();
        if (descriptors == null) return;
        for (ColumnDescriptor columnDescriptor : descriptors)
        {
            statusColumns.add (columnDescriptor);
        }
    }

    @SuppressWarnings({"FeatureEnvy"})
    private void initColumnFields ()
    {
        final int columnsCount = columns.size ();
        if (columnsCount > 0)
        {
            StringBuilder types = new StringBuilder ();
            StringBuilder widths = new StringBuilder ();
            columnTitles = new String[columnsCount];

            for (int i = 0; i < columnsCount; i++)
            {
                NamedColumnDescriptor columnDescriptor = columns.get (i);

                types.append (columnDescriptor.getType ().value ());
                types.append (',');

                widths.append (columnDescriptor.getWidth ());
                widths.append (',');

                columnTitles[i] = columnDescriptor.getTitle ();
            }
            columnTypes = cutTrailingComma (types);
            columnWidths = cutTrailingComma (widths);
        }

        final int statusColumnsCount = statusColumns.size ();
        if (statusColumnsCount > 0)
        {
            StringBuilder types = new StringBuilder ();
            StringBuilder widths = new StringBuilder ();

            for (int i = 0; i < statusColumnsCount; i++)
            {
                ColumnDescriptor columnDescriptor = statusColumns.get (i);

                types.append (columnDescriptor.getType ().value ());
                types.append (',');

                widths.append (columnDescriptor.getWidth ());
                widths.append (',');
            }
            statusColumnTypes = cutTrailingComma (types);
            statusColumnWidths = cutTrailingComma (widths);
        }
    }

    private static String cutTrailingComma (StringBuilder types)
    {
        return types.substring (0, types.length () - 1);
    }

    // ==================================================================================
    // Fields used from native code - not a user API
    // ==================================================================================

    /**
     * "char *ColumnTypes"
     * "Null-terminated text string specifying the column types of the current view mode.
     * It must be in the same format as in FAR configuration,
     * for example something like 'N,S,D,T'.
     * If you wish to use a standard view mode, set ColumnTypes to NULL."
     */
    @SuppressWarnings({"UNUSED_SYMBOL", "FieldCanBeLocal", "UnusedDeclaration"})
    @UsedFromNativeCode
    private String columnTypes;

    /**
     * "char *ColumnWidths"
     * "Null-terminated text string specifying the column widths of the current view mode.
     * It must be in the same format as in FAR configuration,
     * for example something like '0,8,0,5'."
     */
    @SuppressWarnings({"UNUSED_SYMBOL", "FieldCanBeLocal", "UnusedDeclaration"})
    @UsedFromNativeCode
    private String columnWidths;

    /**
     * "char **ColumnTitles"
     * "Points to array with column title addresses.
     * If you wish to use standard titles, set this member to NULL."
     */
    @SuppressWarnings({"UNUSED_SYMBOL", "FieldCanBeLocal"})
    @UsedFromNativeCode
    private String[] columnTitles;

    /**
     * "int FullScreen"
     * "If nonzero, the panel will be shown in the fullscreen mode."
     */
    @SuppressWarnings({"UNUSED_SYMBOL", "FieldCanBeLocal", "UnusedDeclaration"})
    @UsedFromNativeCode
    private int fullScreen;

    /**
     * "int DetailedStatus"
     * "If nonzero, the panel status line will be shown in the detailed format
     * (showing the file name, size, date and time).
     * Otherwise, only the file name will be shown in the status line.
     * This field is used only if StatusColumnTypes and StatusColumnWidths are equal to NULL."
     */
    @SuppressWarnings({"UNUSED_SYMBOL", "FieldCanBeLocal", "UnusedDeclaration"})
    @UsedFromNativeCode
    private int detailedStatus;

    /**
     * "int AlignExtensions"
     * "If nonzero, file extensions will be aligned."
     */
    @SuppressWarnings({"UNUSED_SYMBOL", "FieldCanBeLocal", "UnusedDeclaration"})
    @UsedFromNativeCode
    private int alignExtensions;

    /**
     * "int CaseConversion"
     * "If zero, all file names case conversions will be disabled."
     */
    @SuppressWarnings({"UNUSED_SYMBOL", "FieldCanBeLocal", "UnusedDeclaration"})
    @UsedFromNativeCode
    private int caseConversion;


    /**
     * "char *StatusColumnTypes"
     * "Similar to ColumnTypes, but describes panel status line column types.
     * If you wish to use the standard status line, set StatusColumnTypes to NULL."
     */
    @SuppressWarnings({"UNUSED_SYMBOL", "FieldCanBeLocal", "UnusedDeclaration"})
    @UsedFromNativeCode
    private String statusColumnTypes;


    /**
     * "char *StatusColumnWidths"
     * "Similar to ColumnWidths, but describes panel status line column widths."
     */
    @SuppressWarnings({"UNUSED_SYMBOL", "FieldCanBeLocal", "UnusedDeclaration"})
    @UsedFromNativeCode
    private String statusColumnWidths;

    // "Reserved"
    // "Reserved for future usage. Set it to 0."
    // Not used
}
