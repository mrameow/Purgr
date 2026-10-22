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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.purgr.app.MediaItem
import com.purgr.app.PurgrViewModel
import com.purgr.app.Screen

/** Everything Purgr (or anything else) has moved to the system trash: restore it or delete it for good. */
@Composable
fun TrashScreen(vm: PurgrViewModel, launch: (IntentSender, (Boolean) -> Unit) -> Unit) {
    val selected = remember { mutableStateListOf<Long>() }
    val chosen = vm.trashed.filter { it.id in selected }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.screen = Screen.Swipe }) { Text("< Back") }
            Box(Modifier.weight(1f))
            TextButton(onClick = {
                if (selected.size == vm.trashed.size) selected.clear()
                else { selected.clear(); selected.addAll(vm.trashed.map { it.id }) }
            }) { Text(if (selected.size == vm.trashed.size && selected.isNotEmpty()) "Select none" else "Select all") }
        }
        Text("Trash", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Deleted by mistake? Select items and restore them. Android empties the trash on its own after about 30 days.",
            Modifier.padding(top = 4.dp, bottom = 12.dp),
            color = Color.Gray,
        )

        if (vm.trashed.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Trash is empty", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(vm.trashed, key = { it.id }) { item ->
                    TrashCell(item, item.id in selected) {
                        if (!selected.remove(item.id)) selected.add(item.id)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    launch(vm.restoreRequest(chosen)) { ok ->
                        if (ok) { selected.clear(); vm.reload(); vm.loadTrash() }
                    }
                },
                enabled = chosen.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) { Text("Restore (${chosen.size})") }
            OutlinedButton(
                onClick = {
                    launch(vm.deleteForeverRequest(chosen)) { ok ->
                        if (ok) { selected.clear(); vm.loadTrash() }
                    }
                },
                enabled = chosen.isNotEmpty(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed),
            ) { Text("Delete forever") }
        }
    }
}

@Composable
private fun TrashCell(item: MediaItem, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .then(if (selected) Modifier.border(3.dp, KeepGreen, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selected) {
            Icon(
                Icons.Default.Check, "Selected", tint = Color.Black,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp)
                    .background(KeepGreen, CircleShape).padding(2.dp),
            )
        }
    }
}
