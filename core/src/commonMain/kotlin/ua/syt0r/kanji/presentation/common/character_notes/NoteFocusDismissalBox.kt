package ua.syt0r.kanji.presentation.common.character_notes

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

internal class NoteFocusBounds {
    var editor: Rect? = null
    var root: LayoutCoordinates? = null
}

internal val LocalNoteFocusBounds = staticCompositionLocalOf<NoteFocusBounds?> { null }

@Composable
fun NoteFocusDismissalBox(content: @Composable () -> Unit) {
    val bounds = remember { NoteFocusBounds() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    CompositionLocalProvider(LocalNoteFocusBounds provides bounds) {
        Box(
            Modifier.fillMaxSize()
                .onGloballyPositioned { bounds.root = it }
                .pointerInput(focusManager, keyboard) {
                    awaitEachGesture {
                        // Observe before children handle the tap, without consuming it.
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        val root = bounds.root
                        val editor = bounds.editor
                        if (root != null && root.isAttached && editor != null &&
                            !editor.contains(root.localToWindow(down.position))
                        ) {
                            focusManager.clearFocus()
                            keyboard?.hide()
                        }
                    }
                }
        ) { content() }
    }
}
