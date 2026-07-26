package dev.cinderhell.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.cinderhell.ui.theme.CinderhellPalette
import kotlinx.coroutines.android.awaitFrame

internal enum class ControllerButtonRole {
    PRIMARY,
    SECONDARY,
    QUIET,
    DANGER,
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun ControllerButton(
    id: String,
    focusedId: String?,
    onFocused: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    role: ControllerButtonRole = ControllerButtonRole.SECONDARY,
    content: @Composable RowScope.() -> Unit,
) {
    val requester = remember(id) { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    val interactionSource = remember(id) { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
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
    val restingColor = when (role) {
        ControllerButtonRole.PRIMARY -> CinderhellPalette.Ember
        ControllerButtonRole.SECONDARY -> if (selected) {
            CinderhellPalette.EmberDeep
        } else {
            CinderhellPalette.RaisedIron
        }
        ControllerButtonRole.QUIET -> CinderhellPalette.Iron
        ControllerButtonRole.DANGER -> CinderhellPalette.Error.copy(alpha = 0.24f)
    }
    val focusedColor = when (role) {
        ControllerButtonRole.DANGER -> CinderhellPalette.Error
        else -> CinderhellPalette.EmberBright
    }
    val containerColor by animateColorAsState(
        targetValue = when {
            focused -> focusedColor
            pressed -> CinderhellPalette.EmberDeep
            else -> restingColor
        },
        label = "controller-button-container",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            focused || role == ControllerButtonRole.PRIMARY ->
                CinderhellPalette.Void
            role == ControllerButtonRole.DANGER ->
                CinderhellPalette.Error
            else ->
                CinderhellPalette.Ash
        },
        label = "controller-button-content",
    )
    val borderWidth by animateDpAsState(
        targetValue = when {
            focused -> 3.dp
            selected -> 2.dp
            else -> 1.dp
        },
        label = "controller-button-border",
    )
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.985f
            focused -> 1.012f
            else -> 1f
        },
        label = "controller-button-scale",
    )
    val borderColor = when {
        focused -> CinderhellPalette.Ash
        selected -> CinderhellPalette.Ember
        role == ControllerButtonRole.DANGER -> CinderhellPalette.Error.copy(alpha = 0.72f)
        else -> CinderhellPalette.CoalLine
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(id)
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused(id)
            }
            .semantics {
                this.selected = selected
                stateDescription = buildList {
                    if (selected) add("Selected")
                    if (focused) add("Focused")
                    if (!enabled) add("Unavailable")
                }.ifEmpty {
                    listOf("Available")
                }.joinToString()
            },
        border = BorderStroke(borderWidth, borderColor),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = CinderhellPalette.Iron.copy(alpha = 0.52f),
            disabledContentColor = CinderhellPalette.MutedAsh.copy(alpha = 0.46f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 5.dp else 2.dp,
            pressedElevation = 1.dp,
            focusedElevation = 9.dp,
            disabledElevation = 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        content = content,
    )
}
