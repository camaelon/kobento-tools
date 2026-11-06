package dev.kobento.showcases

import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var account: Account
    /** The sign-in's answer (dev.kobento.showcases:/oauth?code=...), until App has handled it. */
    private val redirect = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        account = Account(this)
        // `adb shell am start -n dev.kobento.showcases/.MainActivity --es showcase SHW-23` opens one;
        // `--es server http://localhost:8899` signs in to another server (a local one, for tests).
        val start = intent.getStringExtra(EXTRA_SHOWCASE)
        DpHost.enabled = intent.getBooleanExtra("dp", true)
        intent.getStringExtra(EXTRA_SERVER)?.let { account.useServer(it) }
        takeRedirect(intent)
        setContent { ShowcasesTheme { App(start, account, redirect) } }
    }

    // singleTask: the Custom Tab's redirect comes back to this activity.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeRedirect(intent)
    }

    private fun takeRedirect(intent: Intent?) {
        val uri = intent?.data
        if (intent?.action == Intent.ACTION_VIEW && account.isRedirect(uri)) redirect.value = uri
    }

    companion object {
        const val EXTRA_SHOWCASE = "showcase"
        const val EXTRA_SERVER = "server"
    }
}

@Composable
private fun App(start: String?, account: Account, redirect: MutableState<Uri?>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showcases = remember { loadShowcases(context.assets) }
    var openId by rememberSaveable { mutableStateOf(start) }
    var openPageId by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pages by remember { mutableStateOf<List<PublishedPage>?>(null) }

    // Runs one account step, with the spinner and the error line.
    fun run(step: suspend () -> Unit) {
        scope.launch {
            busy = true; error = null
            try { step() } catch (e: Account.SignedOut) { pages = null; error = e.message } catch (e: Exception) { error = e.message ?: e.toString() }
            busy = false
        }
    }
    val refresh = { run { pages = account.pages().pages } }

    // The sign-in's answer: tokens, then the pages.
    val answer = redirect.value
    LaunchedEffect(answer) {
        if (answer != null) {
            redirect.value = null
            run { account.finishSignIn(answer); pages = account.pages().pages }
        }
    }
    // Signed in from before: list the pages once.
    LaunchedEffect(account.email) { if (account.email != null && pages == null && answer == null) refresh() }

    val openPage = pages?.firstOrNull { it.id == openPageId }
    val open = showcases.firstOrNull { it.id == openId }
    when {
        openPage != null -> {
            BackHandler { openPageId = null }
            PagePlayerScreen(openPage, onBack = { openPageId = null })
        }
        open != null -> {
            BackHandler { openId = null }
            PlayerScreen(open, onBack = { openId = null })
        }
        else -> GalleryScreen(
            showcases,
            AccountUi(
                email = account.email, busy = busy, error = error, pages = pages,
                onSignIn = { run { account.startSignIn(context) } },
                onSignOut = { run { account.signOut(); pages = null } },
                onRefresh = refresh,
                onOpenPage = { openPageId = it.id },
            ),
            onOpen = { openId = it.id },
        )
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
