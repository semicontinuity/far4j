package org.farmanager.plugins.jdbc;

import org.apache.log4j.Logger;
import org.farmanager.api.dialogs.FarDialog;
import org.farmanager.api.dialogs.FarListBox;
import org.farmanager.api.jni.FarListItem;

/**
 *
 */
public class ItemSelectionDialog extends FarDialog
{
    private static final Logger LOGGER = Logger.getLogger (ParametersDialog.class);

    private FarListBox listBox;

    /**
     * @param title
     * @param data
     */
    public ItemSelectionDialog (final String title, String[] data)
    {
        x1 = -1;
        y1 = -1;
        x2 = 76;
        y2 = 15;

        addItem (listBox = listBox(title, data));
        LOGGER.info ("Dialog constructed");
    }


    private FarListBox listBox (final String title, String[] items)
    {
        final FarListBox box = new FarListBox ();
        box.x1 = 3;
        box.y1 = 1;
        box.x2 = 72;
        box.y2 = 13;
        box.data = title;

        addItems(box, items);

        return box;
    }

    private void addItems (FarListBox farListBox, String[] data)
    {
        final int count = data.length;
        farListBox.items = new FarListItem[count];
        for (int i = 0; i < count; i++)
        {
            final FarListItem listItem = new FarListItem(0, data[i]);
            farListBox.items[i]  = listItem;
        }
    }

    public int selectedItem ()
    {
        return listBox.selectedIndex;
    }
}
