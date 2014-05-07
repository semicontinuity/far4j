package org.farmanager.api.messages;

import org.farmanager.api.AbstractPlugin;
import org.apache.log4j.Logger;

public class Messages {
    private static final Logger LOGGER = Logger.getLogger(Messages.class);

    /**
     * TODO: convert to LOG4J Appender
     */
    public static void showException(Throwable e) {
        LOGGER.error(e, e); // TODO: ideally, all these exceptions should be shown by someone else
        message("Exception", e.getClass().getName() + " " + e.getMessage());
    }

    /**
     * TODO: convert to LOG4J Appender
     */
    public static void longOperation(String m) {
        message("Please wait", m);
    }

    private static void message(String title, String m) {
        final int hScreen = AbstractPlugin.saveScreen();
        AbstractPlugin.message(0, null, new String[] {title, m}, 0);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO?
        }
        AbstractPlugin.restoreScreen(hScreen);
    }
}
