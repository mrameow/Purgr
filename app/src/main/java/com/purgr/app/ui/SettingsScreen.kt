package com.purgr.app.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.purgr.app.Accent
import com.purgr.app.PurgrViewModel
import com.purgr.app.Screen
import com.purgr.app.Settings
import com.purgr.app.ThemeMode

@Composable
fun SettingsScreen(vm: PurgrViewModel) {
    val dark = LocalPurgr.current.dark
    val accents = Accent.entries.filter { it != Accent.DYNAMIC || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        ScreenHeader(title = "Settings", onBack = { vm.screen = Screen.Swipe })

        SectionLabel("Theme")
        PillTabs(ThemeMode.entries, Settings.themeMode, { it.label }) { Settings.updateThemeMode(it) }

        SectionLabel("Accent")
        accents.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { a ->
                    Swatch(Color(if (dark) a.dark else a.light), Settings.accent == a, a.label) { Settings.updateAccent(a) }
                }
            }
        }

        SectionLabel("Reviewed photos")
        Text(
            "Purgr remembers what you've kept so it doesn't ask twice. Start over to see everything again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        GhostButton("Start over with all photos", onClick = { vm.startOver(); vm.screen = Screen.Swipe })
    }
}
