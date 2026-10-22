package com.purgr.app.ui

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purgr.app.PurgrViewModel
import com.purgr.app.Screen

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@Composable
fun PurgrApp() {
    val context = LocalContext.current
    fun granted() = requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    var hasAccess by remember { mutableStateOf(granted()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Android 14+ may grant "selected photos only"; any grant of the media permissions is enough to start.
        hasAccess = granted() || it.values.any { ok -> ok }
    }

    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        if (!hasAccess) {
            PermissionScreen { permissionLauncher.launch(requiredPermissions()) }
        } else {
            MainFlow()
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Purgr", style = MaterialTheme.typography.displayMedium)
        Text(
            "Swipe right to keep, swipe left to delete.\n\nPurgr needs access to your photos and videos. " +
                "Nothing is deleted until you review and confirm at the end of a session.",
            Modifier.padding(vertical = 24.dp),
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRequest) { Text("Allow access") }
    }
}

@Composable
private fun MainFlow() {
    val vm: PurgrViewModel = viewModel()

    // One launcher serves every system trash/delete/restore dialog; the callback is set per request.
    var onResult by remember { mutableStateOf<(Boolean) -> Unit>({}) }
    val sender = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        onResult(it.resultCode == Activity.RESULT_OK)
    }
    val launch: (IntentSender, (Boolean) -> Unit) -> Unit = { intent, callback ->
        onResult = callback
        sender.launch(IntentSenderRequest.Builder(intent).build())
    }

    BackHandler(enabled = vm.screen != Screen.Swipe) { vm.screen = Screen.Swipe }

    if (vm.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    when (vm.screen) {
        Screen.Swipe -> SwipeScreen(vm)
        Screen.Review -> ReviewScreen(vm, launch)
        Screen.Trash -> TrashScreen(vm, launch)
    }
}
