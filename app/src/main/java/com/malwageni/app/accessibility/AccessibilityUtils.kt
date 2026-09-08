package com.malwageni.app.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/**
 * Utility functions and modifiers for TalkBack / Screen Reader accessibility.
 */
object AccessibilityUtils {

    /**
     * Announces a critical message directly to the screen reader (TalkBack).
     * Useful for asynchronous events like network changes or completed transactions.
     */
    fun announceForAccessibility(context: Context, message: String) {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        if (accessibilityManager?.isEnabled == true) {
            val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT).apply {
                text.add(message)
                className = AccessibilityUtils::class.java.name
                packageName = context.packageName
            }
            accessibilityManager.sendAccessibilityEvent(event)
        }
    }
}

/**
 * Compose modifier to mark an element as a LiveRegion for Screen Readers.
 * When content changes inside this composable, TalkBack will politely or assertively
 * announce the updated text without interrupting current speech inappropriately.
 */
fun Modifier.accessibleLiveRegion(
    description: String,
    mode: LiveRegionMode = LiveRegionMode.Polite
): Modifier = this.semantics {
    liveRegion = mode
    contentDescription = description
}
