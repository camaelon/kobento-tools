package dev.kobento.showcases

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the gallery shows of the account: signed out, signing in, or signed in with its pages. */
data class AccountUi(
    val email: String?,
    val busy: Boolean,
    val error: String?,
    val pages: List<PublishedPage>?,
    val onSignIn: () -> Unit,
    val onSignOut: () -> Unit,
    val onRefresh: () -> Unit,
    val onOpenPage: (PublishedPage) -> Unit,
)

@Composable
fun GalleryScreen(showcases: List<Showcase>, account: AccountUi, onOpen: (Showcase) -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 168.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = insets.calculateTopPadding() + 16.dp,
                bottom = insets.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(bottom = 4.dp)) {
                    AccountRow(account)
                    Text("Kobento showcases", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${showcases.size} RemoteCompose documents, played by the androidx player. " +
                            "Tap one to run it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val pages = account.pages
            if (account.email != null && pages != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(if (pages.isEmpty()) "Your pages: none published yet. Pages you publish with Kobento's MCP tools appear here."
                        else "Your pages", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                }
                items(pages, key = { "page-" + it.id }) { PageCard(it, onClick = { account.onOpenPage(it) }) }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Showcases", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                }
            }
            items(showcases, key = { it.id }) { ShowcaseCard(it, onClick = { onOpen(it) }) }
        }
    }
}

@Composable
private fun ShowcaseCard(showcase: Showcase, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(3f / 4f).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            val thumb by thumbnail(showcase)
            thumb?.let {
                Image(it, contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(6.dp)))
            }
            if (showcase.unsupported != null) {
                Text(
                    "Kobento 3D",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(showcase.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                showcase.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Sign in, or who is signed in (and sign out); a sign-in error under it. */
@Composable
private fun AccountRow(account: AccountUi) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(account.email?.let { "Signed in as $it" } ?: "Sign in to see the pages you published",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            when {
                account.busy -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                account.email == null -> Button(onClick = account.onSignIn) { Text("Sign in") }
                else -> {
                    TextButton(onClick = account.onRefresh) { Text("Refresh") }
                    TextButton(onClick = account.onSignOut) { Text("Sign out") }
                }
            }
        }
        account.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun PageCard(page: PublishedPage, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(3f / 4f).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            val preview by produceState<ImageBitmap?>(null, page.previewUrl) {
                value = page.previewUrl?.let { u ->
                    runCatching { download(u).let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }.getOrNull()
                }
            }
            preview?.let {
                Image(it, contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(6.dp)))
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(page.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(page.created.take(10).ifBlank { null }, page.rcBytes.takeIf { it > 0 }?.let(::formatBytes)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The showcase's render in the system's light or dark, decoded off the main thread. */
@Composable
private fun thumbnail(showcase: Showcase) = LocalContext.current.let { context ->
    val file = if (isSystemInDarkTheme()) "render-dark.png" else "render-light.png"
    produceState<ImageBitmap?>(null, showcase.folder, file) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(showcase.asset(file)).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
}
