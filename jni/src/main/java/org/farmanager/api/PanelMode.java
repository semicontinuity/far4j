package org.farmanager.api;

import org.farmanager.api.jni.UsedFromNativeCode;
import org.farmanager.api.panels.ColumnDescriptor;
import org.farmanager.api.panels.NamedColumnDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Corresponds to FAR panel mode
 * Passed to native code
 * TODO: All low-level code (native wrappers) should be moved to jni package!
 */
@SuppressWarnings({"ClassWithTooManyFields"})
@UsedFromNativeCode
public class PanelMode {

    /**
     * Corresponds to PANELMODE_FLAGS.
     */
    public static interface Flags {
        /** Display full screen panel, instead of half-screen */
        long FULLSCREEN      = 0x0000000000000001L;
        /** Display name, size, date and time in status line (if status column types and widths are not specified */
        long DETAILEDSTATUS  = 0x0000000000000002L;
        /** Align file name extensions */
        long ALIGNEXTENSIONS = 0x0000000000000004L;
        /** Convert the case of file names */
        long CASECONVERSION  = 0x0000000000000008L;
    }

    // ==================================================================================
    // Fields used from native code - not a user API
    // ==================================================================================

    /**
     * "const wchar_t *ColumnTypes"
     * "Null-terminated text string specifying the column types of the current view mode.
     * It must be in the same format as in FAR configuration,
     * for example something like 'N,S,D,T'.
     * If you wish to use a standard view mode, set ColumnTypes to NULL."
     */
    @UsedFromNativeCode
    private String columnTypes;

    /**
     * "const wchar_t *ColumnWidths"
     * "Null-terminated text string specifying the column widths of the current view mode.
     * It must be in the same format as in FAR configuration,
     * for example something like '0,8,0,5'.
     * If width == 0, the default column width is used.
     * For the correct operation, it is recommended to have at least one column with width 0.
     * ..."
     */
    @UsedFromNativeCode
    private String columnWidths;

    /**
     * "const wchar_t * const *ColumnTitles"
     * "Points to array with column title addresses.
     * If you wish to use standard titles, set this member to NULL."
     */
    @UsedFromNativeCode
    private String[] columnTitles;

    /**
     * "PANELMODE_FLAGS Flags"
     * Combination of constants declared in {@linkplain org.farmanager.api.PanelMode.Flags Flags} class.
     */
    @UsedFromNativeCode
    private long flags;

    /**
     * "wchar_t *StatusColumnTypes"
     * "Similar to ColumnTypes, but describes panel status line column types.
     * If you wish to use the standard status line, set StatusColumnTypes to NULL."
     */
    @UsedFromNativeCode
    private String statusColumnTypes;


    /**
     * "wchar_t *StatusColumnWidths"
     * "Similar to ColumnWidths, but describes panel status line column widths."
     */
    @UsedFromNativeCode
    private String statusColumnWidths;


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
        flags = fullScreen ? Flags.FULLSCREEN : 0;
        setColumnDescriptors(columns);
        setStatusColumnDescriptors(statusColumns);
        initColumnFields();
    }


    public void setFullScreen(final boolean fullScreen) {
        if (fullScreen) flags |= Flags.FULLSCREEN; else flags &= ~Flags.FULLSCREEN;
    }

    public void setDetailedStatus(final boolean detailedStatus) {
        if (detailedStatus) flags |= Flags.DETAILEDSTATUS; else flags &= ~Flags.DETAILEDSTATUS;
    }

    public void setAlignExtensions(final boolean alignExtensions) {
        if (alignExtensions) flags |= Flags.ALIGNEXTENSIONS; else flags &= ~Flags.ALIGNEXTENSIONS;
    }

    public void setCaseConversion (final boolean caseConversion) {
        if (caseConversion) flags |= Flags.CASECONVERSION; else flags &= ~Flags.CASECONVERSION;
    }


    public void setColumnDescriptors(final NamedColumnDescriptor[] descriptors) {
        columns.clear ();
        if (descriptors == null) return;
        Collections.addAll(columns, descriptors);
    }

    public NamedColumnDescriptor getColumnDescriptor(final int index) {
        return columns.get (index);
    }


    public void setStatusColumnDescriptors(final ColumnDescriptor[] descriptors) {
        statusColumns.clear ();
        if (descriptors == null) return;
        Collections.addAll(statusColumns, descriptors);
    }


    @SuppressWarnings({"FeatureEnvy"})
    private void initColumnFields () {
        final int columnsCount = columns.size ();
        if (columnsCount > 0) {
            final StringBuilder types = new StringBuilder ();
            final StringBuilder widths = new StringBuilder ();
            columnTitles = new String[columnsCount];

            for (int i = 0; i < columnsCount; i++) {
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
        if (statusColumnsCount > 0) {
            final StringBuilder types = new StringBuilder ();
            final StringBuilder widths = new StringBuilder ();

            for (ColumnDescriptor columnDescriptor : statusColumns) {
                types.append(columnDescriptor.getType().value());
                types.append(',');

                widths.append(columnDescriptor.getWidth());
                widths.append(',');
            }
            statusColumnTypes = cutTrailingComma (types);
            statusColumnWidths = cutTrailingComma (widths);
        }
    }

    private static String cutTrailingComma(final StringBuilder s) {
        return s.substring(0, s.length() - 1);
    }
}
