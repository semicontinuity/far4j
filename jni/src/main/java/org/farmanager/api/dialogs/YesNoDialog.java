package org.farmanager.api.dialogs;

import org.apache.log4j.Logger;

public class YesNoDialog extends FarDialog {
    private static final Logger LOGGER = Logger.getLogger(YesNoDialog.class);
    private static final int BUTTONS_Y = 5;
    private static final int WIDTH = 76;
    private FarButton cancelButton;


    public YesNoDialog(
        final String title,
        final String question,
        final String yesButtonText,
        final String noButtonText)
    {
        x1 = -1;
        y1 = -1;
        x2 = WIDTH;
        y2 = 9;

        addItem (title(title));
        addItem (text(question));
        addItem (button(yesButtonText, 28));
        cancelButton = button(noButtonText, 36);
        addItem (cancelButton);
    }


    private static FarDoubleBox title(final String title){
        final FarDoubleBox doubleBox = new FarDoubleBox ();
        doubleBox.x1 = 3;
        doubleBox.y1 = 1;
        doubleBox.x2 = WIDTH-4;
        doubleBox.y2 = 7;
        doubleBox.data = title;
        return doubleBox;
    }

    private static FarText text(final String s) {
        FarText farText = new FarText ();
        farText.setCenterText(true);
        farText.x1 = 5;
        farText.y1 = 3;
        farText.x2 = WIDTH - 8;
        farText.y2 = 3;
        farText.data = s;
        return farText;
    }

    private static FarButton button(final String text, final int x) {
        FarButton button = new FarButton ();
        button.data = text;
        button.x1 = x;
        button.y1 = BUTTONS_Y;
        button.x2 = x + text.length() + 4;
        button.y2 = BUTTONS_Y;
        return button;
    }

    public boolean activate () {
        LOGGER.debug("Activate");
        final boolean b = show() == 2;
        if (b) LOGGER.debug("Confirmed");
        return b; // hardcode: button #2 is OK
    }
}
