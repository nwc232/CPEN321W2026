package com.example.cpen321application

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

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

private sealed interface ScreenState {
    data object SignedOut : ScreenState
    data object Loading : ScreenState
    data class SignedIn(val data: ButtonOneData) : ScreenState
    data class Failed(val message: String) : ScreenState
}

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ScreenState>(ScreenState.SignedOut) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val s = state) {
            is ScreenState.SignedOut -> {
                Button(onClick = {
                    scope.launch {
                        state = ScreenState.Loading
                        state = performSignInAndFetch(context)
                    }
                }) {
                    Text("Sign in with Google")
                }
            }

            is ScreenState.Loading -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Authenticating and contacting server…")
            }

            is ScreenState.SignedIn -> InfoTable(s.data)

            is ScreenState.Failed -> {
                Text("Sign-in failed: ${s.message}")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { state = ScreenState.SignedOut }) {
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

private suspend fun performSignInAndFetch(context: Context): ScreenState {
    val credential: GoogleIdTokenCredential = try {
        signInWithGoogle(context)
    } catch (e: NoCredentialException) {
        return ScreenState.Failed("No Google account on this device.")
    } catch (e: GetCredentialException) {
        return ScreenState.Failed("${e.javaClass.simpleName}: ${e.message}")
    } ?: return ScreenState.Failed("Unexpected credential type")

    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    return try {
        val serverIp = JSONObject(httpGet("$base/api/server-ip")).getString("ip")
        val clientIp = JSONObject(httpGet("$base/api/client-ip")).getString("ip")
        val serverTime = JSONObject(httpGet("$base/api/server-time")).getString("time")
        val nameJson = JSONObject(httpGet("$base/api/name"))
        val clientTime = ZonedDateTime.now()
            .format(DateTimeFormatter.ofPattern("HH:mm:ss 'GMT'xxx"))

        ScreenState.SignedIn(
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
        ScreenState.Failed("Backend error: ${e.message ?: e.javaClass.simpleName}")
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