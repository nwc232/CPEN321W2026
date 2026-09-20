package com.example.cpen321application

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                AppRoot()
            }
        }
    }
}

private enum class Screen { Home, ButtonOne, ButtonTwo, ButtonThree }

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(Screen.Home) }

    when (screen) {
        Screen.Home -> HomeScreen(onNavigate = { screen = it })

        Screen.ButtonOne -> ButtonScaffold(
            title = "Login + Server",
            onBack = { screen = Screen.Home },
        ) { m -> LoginScreen(modifier = m) }

        Screen.ButtonTwo -> ButtonScaffold(
            title = "Live Updates",
            onBack = { screen = Screen.Home },
        ) { m -> LiveUpdatesScreen(modifier = m) }

        Screen.ButtonThree -> ButtonScaffold(
            title = "Timer",
            onBack = { screen = Screen.Home },
        ) { m -> TimerScreen(modifier = m) }
    }
}

@Composable
private fun HomeScreen(onNavigate: (Screen) -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("CPEN 321 — M1", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onNavigate(Screen.ButtonOne) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Login + Server") }
            Button(
                onClick = { onNavigate(Screen.ButtonTwo) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Live Updates") }
            Button(
                onClick = { onNavigate(Screen.ButtonThree) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Timer") }
        }
    }
}

@Composable
private fun ButtonScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("← Back") }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider()
            content(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text)
    }
}

// ---------------------------------------------------------------------------
// Button 1: Login + Server info
// ---------------------------------------------------------------------------

data class ButtonOneData(
    val loggedInFirst: String,
    val loggedInLast: String,
    val backendFirst: String,
    val backendLast: String,
    val serverIp: String,
    val clientIp: String,
    val serverTime: String,
    val clientTime: String,
)

private sealed interface LoginState {
    data object SignedOut : LoginState
    data object Loading : LoginState
    data class SignedIn(val data: ButtonOneData) : LoginState
    data class Failed(val message: String) : LoginState
}

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<LoginState>(LoginState.SignedOut) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val s = state) {
            is LoginState.SignedOut -> {
                Button(onClick = {
                    scope.launch {
                        state = LoginState.Loading
                        state = performSignInAndFetch(context)
                    }
                }) {
                    Text("Sign in with Google")
                }
            }

            is LoginState.Loading -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Authenticating and contacting server…")
            }

            is LoginState.SignedIn -> InfoTable(s.data)

            is LoginState.Failed -> {
                Text("Sign-in failed: ${s.message}")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { state = LoginState.SignedOut }) {
                    Text("Try again")
                }
            }
        }
    }
}

@Composable
private fun InfoTable(data: ButtonOneData) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Login + Server", style = MaterialTheme.typography.titleLarge)
        HorizontalDivider()
        InfoRow("Server IP address", data.serverIp)
        InfoRow("Client IP address", data.clientIp)
        InfoRow("Server local time", data.serverTime)
        InfoRow("Client local time", data.clientTime)
        InfoRow("Your name (backend)", "${data.backendFirst} ${data.backendLast}")
        InfoRow("Logged-in user", "${data.loggedInFirst} ${data.loggedInLast}")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontWeight = FontWeight.Bold)
        Text(text = value.ifBlank { "—" })
    }
}

private suspend fun performSignInAndFetch(context: Context): LoginState {
    val credential: GoogleIdTokenCredential = try {
        signInWithGoogle(context)
    } catch (e: NoCredentialException) {
        return LoginState.Failed("No Google account on this device.")
    } catch (e: GetCredentialException) {
        return LoginState.Failed("${e.javaClass.simpleName}: ${e.message}")
    } ?: return LoginState.Failed("Unexpected credential type")

    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    return try {
        val serverIp = JSONObject(httpGet("$base/api/server-ip")).getString("ip")
        val clientIp = JSONObject(httpGet("$base/api/client-ip")).getString("ip")
        val serverTime = JSONObject(httpGet("$base/api/server-time")).getString("time")
        val nameJson = JSONObject(httpGet("$base/api/name"))
        val clientTime = ZonedDateTime.now()
            .format(DateTimeFormatter.ofPattern("HH:mm:ss 'GMT'xxx"))

        LoginState.SignedIn(
            ButtonOneData(
                loggedInFirst = credential.givenName ?: "",
                loggedInLast = credential.familyName ?: "",
                backendFirst = nameJson.getString("first"),
                backendLast = nameJson.getString("last"),
                serverIp = serverIp,
                clientIp = clientIp,
                serverTime = serverTime,
                clientTime = clientTime,
            )
        )
    } catch (e: Exception) {
        LoginState.Failed("Backend error: ${e.message ?: e.javaClass.simpleName}")
    }
}

private suspend fun signInWithGoogle(context: Context): GoogleIdTokenCredential? {
    val credentialManager = CredentialManager.create(context)
    // setServerClientId takes the WEB client ID, not the Android one.
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(false)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val result = credentialManager.getCredential(context, request)
    val credential = result.credential
    return if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        GoogleIdTokenCredential.createFrom(credential.data)
    } else {
        null
    }
}

private suspend fun httpGet(urlString: String): String = withContext(Dispatchers.IO) {
    val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 5_000
        readTimeout = 5_000
    }
    try {
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}