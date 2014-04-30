package org.farmanager.api.ofs;

import org.farmanager.api.PanelMode;

public interface FarPresentationAttributes
{
    String getFarPanelTitle ();

    String[] getFarPresentationChildrenProperties ();

    PanelMode[] getFarPresentationPanelModes ();

    String getFarDirectory ();
}
