package org.farmanager.api.jni;

import java.util.UUID;

/**
 * Roughly corresponds to structure PluginMenuItem.
 * (Structure PluginMenuItem is a collection of menu items, while this class represents just one item).
 */
public class PluginMenuItem {

    final UUID guid;
    final String string;

    public PluginMenuItem(UUID guid, String string) {
        this.guid = guid;
        this.string = string;
    }
}
