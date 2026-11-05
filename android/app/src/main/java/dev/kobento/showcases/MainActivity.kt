package dev.kobento.showcases

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // `adb shell am start -n dev.kobento.showcases/.MainActivity --es showcase SHW-23` opens one.
        val start = intent.getStringExtra(EXTRA_SHOWCASE)
        DpHost.enabled = intent.getBooleanExtra("dp", true)
        setContent { ShowcasesTheme { App(start) } }
    }

    companion object {
        const val EXTRA_SHOWCASE = "showcase"
    }
}

@Composable
private fun App(start: String?) {
    val context = LocalContext.current
    val showcases = remember { loadShowcases(context.assets) }
    var openId by rememberSaveable { mutableStateOf(start) }
    val open = showcases.firstOrNull { it.id == openId }
    if (open == null) {
        GalleryScreen(showcases, onOpen = { openId = it.id })
    } else {
        BackHandler { openId = null }
        PlayerScreen(open, onBack = { openId = null })
    }
}

// Kobento's site colours: the warm paper and the deep ink, the mark's blue as the accent.
private val Accent = Color(0xFF409BF0)

@Composable
private fun ShowcasesTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Accent, background = Color(0xFF16141F), surface = Color(0xFF16141F),
            surfaceContainer = Color(0xFF221F2E), surfaceContainerHigh = Color(0xFF2B2838))
    } else {
        lightColorScheme(primary = Color(0xFF1F6FC0), background = Color(0xFFFAF8F5), surface = Color(0xFFFAF8F5),
            surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFF1EEE9))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
