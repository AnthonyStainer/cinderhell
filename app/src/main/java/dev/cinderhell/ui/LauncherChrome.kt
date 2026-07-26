package dev.cinderhell.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.cinderhell.ui.theme.CinderhellPalette
import dev.cinderhell.ui.theme.CinderhellSpacing

@Composable
internal fun CinderhellBackdrop(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        CinderhellPalette.Void,
                        CinderhellPalette.Soot,
                        Color(0xFF1C110B),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = CinderhellPalette.EmberDeep.copy(alpha = 0.13f),
                radius = size.minDimension * 0.72f,
                center = Offset(size.width * 0.92f, size.height * 0.08f),
            )
            val lineColor = CinderhellPalette.CoalLine.copy(alpha = 0.14f)
            var x = -size.height
            while (x < size.width) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, size.height),
                    end = Offset(x + size.height, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
                x += 72.dp.toPx()
            }
        }
        CompositionLocalProvider(
            LocalContentColor provides CinderhellPalette.Ash,
            content = content,
        )
    }
}

@Composable
internal fun EmberPanel(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = if (highlighted) {
            CinderhellPalette.RaisedIron.copy(alpha = 0.96f)
        } else {
            CinderhellPalette.Iron.copy(alpha = 0.90f)
        },
        contentColor = CinderhellPalette.Ash,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = if (highlighted) 2.dp else 1.dp,
            color = if (highlighted) {
                CinderhellPalette.EmberDeep
            } else {
                CinderhellPalette.CoalLine
            },
        ),
        tonalElevation = if (highlighted) 8.dp else 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(CinderhellSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(CinderhellSpacing.Element),
            content = content,
        )
    }
}

@Composable
internal fun RouteHeading(
    eyebrow: String,
    title: String,
    detail: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            eyebrow.uppercase(),
            color = CinderhellPalette.Ember,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        detail?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun SectionHeading(
    title: String,
    detail: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        detail?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
internal fun MetadataPill(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Surface(
        modifier = modifier,
        color = if (accent) {
            CinderhellPalette.EmberDeep.copy(alpha = 0.78f)
        } else {
            CinderhellPalette.Soot.copy(alpha = 0.86f)
        },
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            1.dp,
            if (accent) CinderhellPalette.Ember else CinderhellPalette.CoalLine,
        ),
    ) {
        Text(
            text.uppercase(),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = if (accent) CinderhellPalette.Ash else CinderhellPalette.MutedAsh,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun LauncherStatusBanner(
    notice: LauncherNotice?,
    busy: Boolean,
) {
    if (notice == null && !busy) return
    val tone = notice?.tone ?: LauncherNoticeTone.INFO
    val color = when (tone) {
        LauncherNoticeTone.INFO -> CinderhellPalette.Info
        LauncherNoticeTone.SUCCESS -> CinderhellPalette.Success
        LauncherNoticeTone.WARNING -> CinderhellPalette.Warning
        LauncherNoticeTone.ERROR -> CinderhellPalette.Error
    }
    val label = when (tone) {
        LauncherNoticeTone.INFO -> "INFO"
        LauncherNoticeTone.SUCCESS -> "DONE"
        LauncherNoticeTone.WARNING -> "CHECK"
        LauncherNoticeTone.ERROR -> "ERROR"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CinderhellPalette.Soot.copy(alpha = 0.96f), MaterialTheme.shapes.medium)
            .border(1.dp, color.copy(alpha = 0.72f), MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = color,
            )
        } else {
            Box(
                Modifier
                    .size(9.dp)
                    .background(color, CircleShape),
            )
        }
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
        )
        Text(
            notice?.message ?: "Working…",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun EmptyState(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CinderhellPalette.CoalLine, MaterialTheme.shapes.medium)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun CinderhellWordmark(
    controllerLabel: String,
    controllerConnected: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "CINDERHELL",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                "DOOM, HANDHELD",
                color = CinderhellPalette.Ember,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.weight(1f))
        MetadataPill(
            text = controllerLabel,
            accent = controllerConnected,
        )
    }
}

@Composable
internal fun FooterActions(
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CinderhellSpacing.Element),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
internal fun LoadingLauncher(notice: LauncherNotice?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmberPanel(modifier = Modifier.fillMaxWidth(0.52f), highlighted = true) {
            Text(
                "CINDERHELL",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                "Preparing your library",
                color = CinderhellPalette.Ember,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(6.dp))
            CircularProgressIndicator(color = CinderhellPalette.Ember)
            notice?.let {
                Text(
                    it.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
