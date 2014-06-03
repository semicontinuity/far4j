package org.farmanager.api.jni;

/**
 * Corresponds to FAR structure InfoPanelLine
 */
@UsedFromNativeCode
public class FarInfoPanelLine
{
    public FarInfoPanelLine (final String text, final String data, final boolean separator) {
        this.text = text;
        this.data = data;
        this.separator = separator;
    }

    @UsedFromNativeCode
    private final String text;

    @UsedFromNativeCode
    private final String data;

    @UsedFromNativeCode
    private final boolean separator;


    public String getText() {
        return text;
    }

    public String getData() {
        return data;
    }

    public boolean isSeparator() {
        return separator;
    }
}
