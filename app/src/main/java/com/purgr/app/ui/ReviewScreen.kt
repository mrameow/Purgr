package com.purgr.app.ui

import android.content.IntentSender
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val extras = LocalPurgr.current

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader(
            title = "Review",
            subtitle = "Tap anything to keep it. Nothing is deleted yet.",
            onBack = { vm.screen = Screen.Swipe },
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(vm.marked, key = { it.id }) { item ->
                ReviewCell(item, rescued = item.id in vm.rescued) { vm.toggleRescue(item) }
            }
        }

        PillButton(
            text = if (deleting.isEmpty()) "Nothing to delete" else
                "Delete ${deleting.size} · ${Formatter.formatShortFileSize(context, bytes)}",
            onClick = {
                val intent = vm.beginTrash()
                if (intent == null) {
                    vm.finishWithoutDeleting()
                } else {
                    // The system asks once per batch; we chain batches until all are done.
                    fun run(sender: IntentSender): Unit = launch(sender) { ok ->
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
            container = extras.danger,
            content = Color.White,
        )
        Text(
            "Goes to your phone's Trash first. You can restore it from Purgr's trash.",
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReviewCell(item: MediaItem, rescued: Boolean, onClick: () -> Unit) {
    val extras = LocalPurgr.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, if (rescued) extras.keep else extras.hairline, shape)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(if (rescued) 0.4f else 1f),
        )
        if (item.isVideo) {
            Icon(
                Icons.Default.PlayArrow, "Video", tint = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).size(18.dp),
            )
        }
        if (rescued) CheckBadge(extras.keep, Modifier.align(Alignment.Center))
    }
}
