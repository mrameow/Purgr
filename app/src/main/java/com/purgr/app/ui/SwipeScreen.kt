package com.purgr.app.ui

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.purgr.app.MediaItem
import com.purgr.app.PurgrViewModel
import com.purgr.app.Screen
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeScreen(vm: PurgrViewModel) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var busy by remember { mutableStateOf(false) }
    val top = vm.queue.firstOrNull()
    val next = vm.queue.getOrNull(1)
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val threshold = widthPx * 0.3f
    val extras = LocalPurgr.current

    fun commit(keep: Boolean) {
        if (busy) return
        busy = true
        scope.launch {
            offsetX.animateTo(if (keep) widthPx * 1.5f else -widthPx * 1.5f, tween(220))
            vm.swipe(keep)
            offsetX.snapTo(0f)
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader(
            title = "Purgr",
            subtitle = if (top == null) null else "${vm.queue.size} left",
        ) {
            if (vm.marked.isNotEmpty()) {
                Text(
                    "Review ${vm.marked.size}",
                    Modifier
                        .clip(CircleShape)
                        .background(extras.danger.copy(alpha = 0.12f))
                        .clickable { vm.screen = Screen.Review }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = extras.danger,
                )
            }
            IconButton(onClick = { vm.loadTrash(); vm.screen = Screen.Trash }) {
                Icon(Icons.Default.Delete, "Trash", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { vm.screen = Screen.Settings }) {
                Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (vm.totalCount > 0) {
            LinearProgressIndicator(
                progress = { ((vm.totalCount - vm.queue.size).toFloat() / vm.totalCount).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = extras.hairline,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }

        if (top == null) {
            DoneState(vm, Modifier.weight(1f))
            return@Column
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
            // Sits underneath, growing as the top card moves away.
            if (next != null) {
                MediaCard(
                    next,
                    Modifier.fillMaxSize().scale(0.95f + 0.05f * (abs(offsetX.value) / threshold).coerceIn(0f, 1f)),
                )
            }

            val progress = (offsetX.value / threshold).coerceIn(-1f, 1f)
            val edge = when {
                progress > 0f -> lerp(extras.hairline, extras.keep, progress)
                progress < 0f -> lerp(extras.hairline, extras.danger, -progress)
                else -> extras.hairline
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .rotate(offsetX.value / widthPx * 10f)
                    .pointerInput(top.id) {
                        detectDragGestures(
                            onDragEnd = {
                                when {
                                    offsetX.value > threshold -> commit(true)
                                    offsetX.value < -threshold -> commit(false)
                                    else -> scope.launch { offsetX.animateTo(0f, tween(180)) }
                                }
                            },
                            onDragCancel = { scope.launch { offsetX.animateTo(0f, tween(180)) } },
                        ) { change, drag ->
                            change.consume()
                            if (!busy) scope.launch { offsetX.snapTo(offsetX.value + drag.x) }
                        }
                    },
            ) {
                MediaCard(top, Modifier.fillMaxSize(), playable = true, edge = edge)
                Stamp("Keep", extras.keep, Alignment.TopStart, progress.coerceAtLeast(0f))
                Stamp("Delete", extras.danger, Alignment.TopEnd, (-progress).coerceAtLeast(0f))
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RingButton(Icons.Default.Close, extras.danger, "Delete") { commit(false) }
            GhostButton("Undo", onClick = { vm.undo() }, enabled = vm.canUndo && !busy)
            RingButton(Icons.Default.Check, extras.keep, "Keep") { commit(true) }
        }
    }
}

@Composable
private fun BoxScope.Stamp(text: String, color: Color, align: Alignment, alpha: Float) {
    if (alpha <= 0f) return
    Text(
        text,
        Modifier
            .align(align)
            .padding(20.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
    )
}

@Composable
fun MediaCard(item: MediaItem, modifier: Modifier = Modifier, playable: Boolean = false, edge: Color = LocalPurgr.current.hairline) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (playable && item.isVideo) {
                    Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(item.uri, "video/*")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(intent)
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.isVideo) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .background(Color(0x66000000), CircleShape),
            )
            Text(
                formatDuration(item.durationMs),
                Modifier.align(Alignment.BottomEnd).padding(14.dp)
                    .background(Color(0x99000000), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
        // Hairline ring drawn on top so it also tints toward green/red while dragging.
        Box(Modifier.fillMaxSize().border(1.dp, edge, shape))
    }
}

@Composable
private fun DoneState(vm: PurgrViewModel, modifier: Modifier) {
    CenteredMessage(
        title = "All caught up",
        subtitle = if (vm.marked.isEmpty()) "Nothing is waiting to be deleted."
        else "${vm.marked.size} item${if (vm.marked.size == 1) " is" else "s are"} waiting for your review.",
        modifier = modifier,
    ) {
        if (vm.marked.isNotEmpty()) {
            PillButton(
                "Review ${vm.marked.size}",
                onClick = { vm.screen = Screen.Review },
                container = LocalPurgr.current.danger,
                content = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        GhostButton("Start over with all photos", onClick = { vm.startOver() })
    }
}

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
