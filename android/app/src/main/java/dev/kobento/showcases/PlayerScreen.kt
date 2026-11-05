package dev.kobento.showcases

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.remote.player.view.RemoteComposePlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(showcase: Showcase, onBack: () -> Unit) {
    var look by rememberSaveable(showcase.id) { mutableStateOf(Look.Auto) }
    var info by rememberSaveable(showcase.id) { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(showcase.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(showcase.id, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { info = !info }) {
                        Icon(painterResource(R.drawable.ic_info), contentDescription = if (info) "Hide details" else "Details")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            AnimatedVisibility(info) {
                Text(
                    showcase.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (l in showcase.looks) FilterChip(selected = l == look, onClick = { look = l }, label = { Text(l.label) })
            }
            var anyway by rememberSaveable(showcase.id) { mutableStateOf(false) }
            if (showcase.unsupported != null && !anyway) {
                Unsupported(showcase.unsupported, Modifier.weight(1f).fillMaxWidth(), onPlay = { anyway = true })
            } else {
                DocumentFrame(showcase, Modifier.weight(1f).fillMaxWidth()) {
                    // A new player per look: each look is its own document (or the same one in another theme).
                    key(look) { Player(showcase, look) }
                }
            }
        }
    }
}

@Composable
private fun Unsupported(reason: String, modifier: Modifier, onPlay: () -> Unit) {
    Column(modifier.padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Not playable here", style = MaterialTheme.typography.titleMedium)
        Text(reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
        TextButton(onClick = onPlay) { Text("Play anyway") }
    }
}

/** The document at the size its form factor wants: a phone or tablet fills the screen and relays out to it. */
@Composable
private fun DocumentFrame(showcase: Showcase, modifier: Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.padding(12.dp), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val frame = when (showcase.formFactor) {
            "watch-round" -> Modifier.size(side * 0.9f).clip(CircleShape)
            "square" -> Modifier.size(side)
            "widget-4x1" -> Modifier.fillMaxWidth().aspectRatio(4f).clip(RoundedCornerShape(16.dp))
            else -> Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
        }
        Box(frame) { content() }
    }
}

@Composable
private fun Player(showcase: Showcase, look: Look) {
    val context = LocalContext.current
    val bytes = remember(showcase.folder, look) { context.assets.open(showcase.asset(look.file)).use { it.readBytes() } }
    val theme = when (look) {
        Look.Auto -> if (isSystemInDarkTheme()) RemoteComposePlayer.THEME_DARK else RemoteComposePlayer.THEME_LIGHT
        Look.Light, Look.MonoLight -> RemoteComposePlayer.THEME_LIGHT
        Look.Dark, Look.MonoDark -> RemoteComposePlayer.THEME_DARK
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { DpHost(it, RemoteComposePlayer(it).apply { setTheme(theme); setDocument(bytes) }) },
        // Auto follows the system: the theme changes in place, the document keeps its state.
        update = { (it.child as RemoteComposePlayer).setTheme(theme) },
    )
}
