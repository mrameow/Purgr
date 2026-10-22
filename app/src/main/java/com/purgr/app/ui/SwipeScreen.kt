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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.offset
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

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TopBar(vm)

        if (top == null) {
            DoneState(vm, Modifier.weight(1f))
            return@Column
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)) {
            // Sits underneath, growing as the top card moves away.
            if (next != null) {
                MediaCard(
                    next,
                    Modifier.fillMaxSize().scale(0.94f + 0.06f * (abs(offsetX.value) / threshold).coerceIn(0f, 1f)),
                )
            }

            val progress = (offsetX.value / threshold).coerceIn(-1f, 1f)
            Box(
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .rotate(offsetX.value / widthPx * 12f)
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
                MediaCard(top, Modifier.fillMaxSize(), playable = true)
                Stamp("KEEP", KeepGreen, Alignment.TopStart, progress.coerceAtLeast(0f), -12f)
                Stamp("DELETE", DeleteRed, Alignment.TopEnd, (-progress).coerceAtLeast(0f), 12f)
            }
        }

        ActionRow(
            canUndo = vm.canUndo && !busy,
            onDelete = { commit(false) },
            onKeep = { commit(true) },
            onUndo = { vm.undo() },
        )
    }
}

@Composable
private fun TopBar(vm: PurgrViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Purgr", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("${vm.queue.size} left to review", color = Color.Gray, fontSize = 12.sp)
        }
        IconButton(onClick = { vm.loadTrash(); vm.screen = Screen.Trash }) {
            Icon(Icons.Default.Delete, contentDescription = "Trash")
        }
        Button(
            onClick = { vm.screen = Screen.Review },
            enabled = vm.marked.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
        ) { Text("Review (${vm.marked.size})") }
    }
}

@Composable
private fun ActionRow(
    canUndo: Boolean,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(Icons.Default.Close, DeleteRed, "Delete", onDelete)
        TextButton(onClick = onUndo, enabled = canUndo) { Text("Undo") }
        RoundButton(Icons.Default.Check, KeepGreen, "Keep", onKeep)
    }
}

@Composable
private fun RoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Surface)
            .border(2.dp, tint, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Stamp(
    text: String,
    color: Color,
    align: Alignment,
    alpha: Float,
    angle: Float,
) {
    Text(
        text,
        Modifier
            .align(align)
            .padding(24.dp)
            .alpha(alpha)
            .rotate(angle)
            .border(3.dp, color, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = color,
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold,
    )
}

@Composable
fun MediaCard(item: MediaItem, modifier: Modifier = Modifier, playable: Boolean = false) {
    val context = LocalContext.current
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
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
                    .size(72.dp)
                    .background(Color(0x66000000), CircleShape),
            )
            Text(
                formatDuration(item.durationMs),
                Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    .background(Color(0x99000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun DoneState(vm: PurgrViewModel, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("All caught up!", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (vm.marked.isEmpty()) "Nothing is waiting to be deleted."
            else "${vm.marked.size} item(s) are waiting for your review.",
            Modifier.padding(vertical = 12.dp),
            color = Color.Gray,
        )
        if (vm.marked.isNotEmpty()) {
            Button(
                onClick = { vm.screen = Screen.Review },
                colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
            ) { Text("Review & delete") }
        }
        TextButton(onClick = { vm.startOver() }) { Text("Start over with all photos") }
    }
}

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
