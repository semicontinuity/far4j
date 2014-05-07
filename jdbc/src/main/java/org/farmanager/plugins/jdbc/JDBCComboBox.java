package org.farmanager.plugins.jdbc;

import org.apache.log4j.Logger;
import org.farmanager.api.dialogs.FarComboBox;
import org.farmanager.api.jni.FarListItem;

import java.util.List;

/**
 * @author Igor A. Karpov (ikar)
 *         TODO: editing is not fully supported!
 */
public class JDBCComboBox extends FarComboBox implements EditedValueProvider {
    private static final Logger LOGGER = Logger.getLogger(QueryPanelContentProvider.class);
    //    private int[] ids;
    public String initialValue;

    public JDBCComboBox(final List<IdValuePair> choices) {
//        flags = FarDialogItemFlags.DIF_DROPDOWNLIST.value();
        items = new FarListItem[choices.size()];
//        ids = new int[choices.size()];
        for (int i = 0; i < items.length; i++) {
            final IdValuePair pair = choices.get(i);
//            ids[i] = pair.getId();
            items[i] = new FarListItem(0, pair.getValue());
        }
    }

    public String getEditedValue() {
        LOGGER.info("getEditedValue");
        LOGGER.info("selectedIndex=" + selectedIndex);
        LOGGER.info("initialValue=" + initialValue);
//        return selectedIndex == -1 ? initialValue : items[selectedIndex].text;
        // selectedIndex (ListPos) stopped working??
        return data;
    }
}
