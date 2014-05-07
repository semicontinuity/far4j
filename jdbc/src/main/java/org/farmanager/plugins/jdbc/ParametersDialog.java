package org.farmanager.plugins.jdbc;

import org.apache.log4j.Logger;
import org.farmanager.api.dialogs.FarButton;
import org.farmanager.api.dialogs.FarDialog;
import org.farmanager.api.dialogs.FarDialogItem;
import org.farmanager.api.dialogs.FarDoubleBox;
import org.farmanager.api.dialogs.FarText;

import java.util.Arrays;
import java.util.Properties;

/**
 * FAR dialog for general-purpose entry of parameters to SQL query
 */
public class ParametersDialog extends FarDialog
{
    private static final Logger LOGGER = Logger.getLogger (ParametersDialog.class);

    private FarButton cancelButton;
    private FarDialogItem[] parameterDataControls;

    private static final int BUTTONS_Y = 12;
    private static final int SEPARATOR_Y = 11;

    /**
     * @param selectedLineValues selected line values (only when editing)
     */
    public ParametersDialog (
            final QueryPanelContentProvider provider,
            final Properties properties,
            final String prefix,
            final String[] selectedLineValues)
    {
        LOGGER.info("Constructing ParametersDialog");
        x1 = -1;
        y1 = -1;
        x2 = 76;
        y2 = 15;

        addItem (addTitle (properties, prefix));
        addParameters (properties, prefix, selectedLineValues, provider);
        addItem (separator ());
        // total width: 74; center: 37; buttons width=18;
        // width = 6
        addItem (okButton ());

        // width = 10
        addItem (cancelButton = cancelButton ());
        LOGGER.info ("Dialog constructed");
    }

    private static FarDoubleBox addTitle (Properties properties, String prefix)
    {
        final FarDoubleBox doubleBox = new FarDoubleBox ();
        doubleBox.x1 = 3;
        doubleBox.y1 = 1;
        doubleBox.x2 = 72;
        doubleBox.y2 = 13;
        doubleBox.data = properties.getProperty (prefix + ".query.title");
        return doubleBox;
    }

    private void addParameters (Properties properties,
            String prefix,
            String[] selectedLineValues, QueryPanelContentProvider provider)
    {
        int count = Integer.parseInt (properties.getProperty (prefix + ".query.param.count"));
        parameterDataControls = new FarDialogItem[count];

        LOGGER.info ("parameter count: " + count);
        for (int i = 0; i < count; i++)
        {
            FarText farText = new FarText ();
            farText.x1 = 5;
            farText.y1 = 3+i;
            farText.x2 = 25;
            farText.y2 = 3+i;
            farText.data = properties.getProperty (prefix + ".query.param." + i);
            addItem (farText);

            final String subQuery =
                    properties.getProperty (prefix + ".query.param." + i + ".query");
            LOGGER.info ("subQuery for parameter " + i + " = " + subQuery);

            if (selectedLineValues != null)
                LOGGER.info("selectedLineValues[i]=" + selectedLineValues[i]);

            if (subQuery != null)
            {
                final JDBCComboBox jdbcComboBox = new JDBCComboBox(provider.executeIdValueQuery(subQuery));
                jdbcComboBox.initialValue = selectedLineValues == null ? "" : selectedLineValues[i];
                parameterDataControls[i] = jdbcComboBox;
            }
            else
            {
                parameterDataControls[i] = new JDBCEditControl ();
            }
            parameterDataControls[i].data =
                    selectedLineValues == null ? "" : selectedLineValues[i];
            parameterDataControls[i].x1 = 26;
            parameterDataControls[i].y1 = 3 + i;
            parameterDataControls[i].x2 = 70;
            parameterDataControls[i].y2 = 3 + i;
            addItem (parameterDataControls[i]);

            LOGGER.info("SET parameterDataControls[i].data=" + parameterDataControls[i].data);
        }
    }


    private static FarText separator ()
    {
        FarText separator = new FarText ();
        separator.x1 = 5;
        separator.y1 = SEPARATOR_Y;
        separator.x2 = 0;
        separator.y2 = 0;
        separator.data = "";
        separator.setBoxColor (true);
        separator.setSeparator (true);
        return separator;
    }

    private static FarButton okButton ()
    {
        FarButton ok = new FarButton ();
        ok.x1 = 28;
        ok.y1 = BUTTONS_Y;
        ok.x2 = 34;
        ok.y2 = BUTTONS_Y;
        ok.data = "OK";
        return ok;
    }

    private static FarButton cancelButton ()
    {
        FarButton cancelButton = new FarButton ();
        cancelButton.x1 = 36;
        cancelButton.y1 = BUTTONS_Y;
        cancelButton.x2 = 36 + 10;
        cancelButton.y2 = BUTTONS_Y;
        cancelButton.data = "Cancel";
        return cancelButton;
    }

    /**
     * Activates the dialog
     * @return true if dialog
     */
    public boolean activate () {
        return (show () != -1) && !cancelButton.selected;
    }

    public Object[] getParams (Object id) {
        final Object[] params = new Object[parameterDataControls.length + 1];
        for (int i=0; i < parameterDataControls.length; i++) {
            params[i+1] = ((EditedValueProvider)parameterDataControls[i]).getEditedValue();
        }
        params[0] = id;
        LOGGER.info("Returning edited values: " + Arrays.toString(params));
        return params;
    }
}
