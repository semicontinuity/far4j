package org.farmanager.api.dialogs;

import org.apache.log4j.Logger;
import org.farmanager.api.AbstractPlugin;
import org.farmanager.api.InitDialogItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents FAR dialog.
 * Contains a list of UI widgets (items).
 * When Dialog function is invoked,
 * for every item an instance of InitDialogItem is constructed (this is perhaps not good)
 */
public class FarDialog extends FarComponent {

    private static final Logger LOGGER = Logger.getLogger (FarDialog.class);

    private List<FarDialogItem> items = new ArrayList<FarDialogItem>();

    /**
     * Adds another UI widget to this dialog
     * @param item UI widget
     */
    public void addItem (final FarDialogItem item)
    {
        items.add (item);
    }

    /**
     * Shows this dialog
     * @return This function returns either -1, if the user cancelled the dialog,
     * or the index of the selected dialog item.
     */
    public int show()
    {
        final InitDialogItem[] initDialogItems = constructInitDialogItems ();
        LOGGER.info ("Going to show dialog...");
        final int code = AbstractPlugin.dialog(
            x1, y1, x2, y2, "helpTopic", initDialogItems);
        getDataFromInitDialogItems (initDialogItems);
        return code;
    }

    private InitDialogItem[] constructInitDialogItems ()
    {
        LOGGER.info ("constructInitDialogItems");
        InitDialogItem[] initDialogItems = new InitDialogItem[items.size()];
        for (int i = 0; i < initDialogItems.length; i++)
        {
            FarDialogItem item = items.get(i);
            LOGGER.info("item.getData()=" + item.getData());
            initDialogItems[i] = new InitDialogItem(
                item.getType(),
                item.x1,
                item.y1,
                item.x2,
                item.y2,
                item.isFocused() ? 1 : 0,
                item.isSelected() ? 1 : 0,
                item.getFlags(),
                0,
                item.getData()
                );
            // TODO: bad
            if (item instanceof FarListControl)
            {               
                FarListControl listControl = (FarListControl) item;
                initDialogItems[i].param = listControl.items;
                LOGGER.info ("FarListControl: "+ listControl.items);
            }
        }
        return initDialogItems;
    }

    private void getDataFromInitDialogItems (InitDialogItem[] initDialogItems)
    {
        for (int i = 0; i < initDialogItems.length; i++)
        {
            FarDialogItem item = items.get(i);
            item.data = initDialogItems[i].data;
            item.selected = initDialogItems[i].selected == 1;

            // TODO: this is ugly workaround
            if (item instanceof FarListControl)
            {
                LOGGER.info("initDialogItems[i].selected = " + initDialogItems[i].selected);
                ((FarListControl)item).selectedIndex = initDialogItems[i].selected;
            }
        }
    }
}
