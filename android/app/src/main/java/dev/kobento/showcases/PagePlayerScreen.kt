package dev.kobento.showcases

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

private val PAGE_LOOKS = listOf(Look.Auto, Look.Light, Look.Dark)

/** One of the account's published pages: its document downloaded and played like a showcase. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PagePlayerScreen(page: PublishedPage, onBack: () -> Unit) {
    var look by rememberSaveable(page.id) { mutableStateOf(Look.Auto) }
    var info by rememberSaveable(page.id) { mutableStateOf(false) }
    var attempt by rememberSaveable(page.id) { mutableIntStateOf(0) }
    val bytes by produceState<Result<ByteArray>?>(null, page.rcUrl, attempt) { value = runCatching { download(page.rcUrl) } }
    val context = LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(page.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Your page", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back") } },
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
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bytes?.getOrNull()?.let { Fact("Size", formatBytes(it.size)) }
                        if (page.width > 0) Fact("Designed at", "${page.width} × ${page.height} dp")
                        if (page.created.length >= 10) Fact("Published", page.created.take(10))
                    }
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, page.url.toUri())) },
                        modifier = Modifier.padding(top = 4.dp)) { Text("Open the web page") }
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (l in PAGE_LOOKS) FilterChip(selected = l == look, onClick = { look = l }, label = { Text(l.label) })
            }
            val result = bytes
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    result == null -> CircularProgressIndicator()
                    result.isFailure -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Could not load this page", style = MaterialTheme.typography.titleMedium)
                        Text(result.exceptionOrNull()?.message.orEmpty(), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                        TextButton(onClick = { attempt++ }) { Text("Try again") }
                    }
                    else -> DocumentFrame("phone", Modifier.fillMaxSize()) { key(look) { Player(result.getOrThrow(), look) } }
                }
            }
        }
    }
}
