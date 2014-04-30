package org.farmanager.api.jni;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to mark code elements that are accessed from native code
 * Do not rename/delete/move such elements without fixing references in native code!
 * @author Igor A. Karpov (ikar)
 * TODO: split into 2: ReadInNativeCode, WrittenInNativeCode?
 */
@Retention(RetentionPolicy.SOURCE)
public @interface UsedFromNativeCode
{
}
