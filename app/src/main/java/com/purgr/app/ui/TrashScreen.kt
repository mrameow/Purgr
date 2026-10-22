package com.purgr.app.ui

import android.content.IntentSender
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import coil.compose.AsyncImage
import com.purgr.app.MediaItem
import com.purgr.app.PurgrViewModel
import com.purgr.app.Screen

/** Everything Purgr (or anything else) has moved to the system trash: restore it or delete it for good. */
@Composable
fun TrashScreen(vm: PurgrViewModel, launch: (IntentSender, (Boolean) -> Unit) -> Unit) {
    val selected = remember { mutableStateListOf<Long>() }
    val chosen = vm.trashed.filter { it.id in selected }
    val allSelected = vm.trashed.isNotEmpty() && selected.size == vm.trashed.size
    val extras = LocalPurgr.current

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader(
            title = "Trash",
            subtitle = "Android empties it after about 30 days.",
            onBack = { vm.screen = Screen.Swipe },
        ) {
            if (vm.trashed.isNotEmpty()) {
                GhostButton(if (allSelected) "None" else "All", onClick = {
                    selected.clear()
                    if (!allSelected) selected.addAll(vm.trashed.map { it.id })
                })
            }
        }

        if (vm.trashed.isEmpty()) {
            CenteredMessage("Trash is empty", "Things you delete will wait here, ready to restore.", Modifier.weight(1f))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(vm.trashed, key = { it.id }) { item ->
                    TrashCell(item, item.id in selected) {
                        if (!selected.remove(item.id)) selected.add(item.id)
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillButton(
                text = "Delete forever",
                onClick = {
                    launch(vm.deleteForeverRequest(chosen)) { ok ->
                        if (ok) { selected.clear(); vm.loadTrash() }
                    }
                },
                enabled = chosen.isNotEmpty(),
                modifier = Modifier.weight(1f),
                container = extras.danger.copy(alpha = 0.12f),
                content = extras.danger,
            )
            PillButton(
                text = if (chosen.isEmpty()) "Restore" else "Restore ${chosen.size}",
                onClick = {
                    launch(vm.restoreRequest(chosen)) { ok ->
                        if (ok) { selected.clear(); vm.reload(); vm.loadTrash() }
                    }
                },
                enabled = chosen.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TrashCell(item: MediaItem, selected: Boolean, onClick: () -> Unit) {
    val extras = LocalPurgr.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else extras.hairline, shape)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selected) CheckBadge(MaterialTheme.colorScheme.primary, Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}
