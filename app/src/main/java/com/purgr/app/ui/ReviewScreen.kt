package com.purgr.app.ui

import android.content.IntentSender
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.purgr.app.MediaItem
import com.purgr.app.PurgrViewModel
import com.purgr.app.Screen

/**
 * End-of-session checkpoint: everything swiped left is listed here. Tap anything to rescue it,
 * then confirm. Only now is anything actually removed (moved to the system trash).
 */
@Composable
fun ReviewScreen(vm: PurgrViewModel, launch: (IntentSender, (Boolean) -> Unit) -> Unit) {
    val context = LocalContext.current
    val deleting = vm.toDelete
    val bytes = deleting.sumOf { it.sizeBytes }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.screen = Screen.Swipe }) { Text("< Keep swiping") }
        }
        Text("Review before deleting", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Tap anything you want to rescue. Nothing has been deleted yet.",
            Modifier.padding(top = 4.dp, bottom = 12.dp),
            color = Color.Gray,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(vm.marked, key = { it.id }) { item ->
                ReviewCell(item, rescued = item.id in vm.rescued) { vm.toggleRescue(item) }
            }
        }

        Button(
            onClick = {
                val intent = vm.beginTrash()
                if (intent == null) {
                    vm.finishWithoutDeleting()
                } else {
                    // The system asks once per batch; we chain batches until all are done.
                    fun run(sender: IntentSender) = launch(sender) { ok ->
                        if (ok) {
                            vm.onChunkTrashed()
                            vm.nextTrashRequest()?.let { run(it) }
                        } else {
                            vm.cancelTrash()
                        }
                    }
                    run(intent)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
        ) {
            Text(
                if (deleting.isEmpty()) "Nothing to delete — done"
                else "Delete ${deleting.size} (${Formatter.formatShortFileSize(context, bytes)})",
            )
        }
        Text(
            "Deleted items go to your phone's Trash first, and stay restorable from Purgr's trash (top right).",
            Modifier.padding(vertical = 8.dp),
            color = Color.Gray,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ReviewCell(item: MediaItem, rescued: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .then(if (rescued) Modifier.border(3.dp, KeepGreen, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(if (rescued) 1f else 0.55f),
        )
        if (item.isVideo) {
            Icon(
                Icons.Default.PlayArrow, "Video", tint = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).size(20.dp),
            )
        }
        if (rescued) {
            Icon(
                Icons.Default.Check, "Rescued", tint = Color.Black,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp)
                    .background(KeepGreen, CircleShape).padding(2.dp),
            )
        }
    }
}
