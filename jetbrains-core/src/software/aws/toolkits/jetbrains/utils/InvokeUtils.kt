// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import java.lang.reflect.InvocationTargetException
import javax.swing.SwingUtilities

/**
 * JetBrains version was marked as internal and deprecated in 2019.1
 */
fun <T> invokeAndWaitIfNeeded(modalityState: ModalityState? = null, runnable: () -> T): T {
    val app = ApplicationManager.getApplication()
    if (app == null) {
        if (SwingUtilities.isEventDispatchThread()) {
            return runnable()
        } else {
            @Suppress("UNCHECKED_CAST")
            try {
                return computeDelegated { SwingUtilities.invokeAndWait { it(runnable()) } }
            } catch (e: InvocationTargetException) {
                throw e.cause ?: e
            }
        }
    } else if (app.isDispatchThread) {
        return runnable()
    } else {
        return computeDelegated {
            app.invokeAndWait(
                { it(runnable()) },
                modalityState ?: ModalityState.defaultModalityState()
            )
        }
    }
}

internal inline fun <T> computeDelegated(executor: (setter: (T) -> Unit) -> Unit): T {
    var resultRef: T? = null
    executor { resultRef = it }
    @Suppress("UNCHECKED_CAST")
    return resultRef as T
}