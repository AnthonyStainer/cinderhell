package dev.cinderhell.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.android.awaitFrame

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun ControllerButton(
    id: String,
    focusedId: String?,
    onFocused: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val requester = remember(id) { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    var focused by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id, focusedId, enabled) {
        if (focusedId == id && enabled) {
            inputModeManager.requestInputMode(InputMode.Keyboard)
            repeat(4) {
                awaitFrame()
                requester.requestFocus()
                if (focused) return@LaunchedEffect
            }
        }
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .testTag(id)
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused(id)
            },
        border = if (focused) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
        } else {
            null
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (focused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            },
        ),
        content = content,
    )
}
