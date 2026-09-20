package com.example.cpen321application

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private enum class TimerPhase { Idle, Running, Finished }

private sealed interface SurpriseState {
    data object Loading : SurpriseState
    data class Loaded(val breed: String, val image: ImageBitmap) : SurpriseState
    data class Failed(val message: String) : SurpriseState
}

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    var minutes by remember { mutableStateOf("0") }
    var seconds by remember { mutableStateOf("10") }
    var phase by remember { mutableStateOf(TimerPhase.Idle) }
    var remaining by remember { mutableStateOf(0) }
    var surprise by remember { mutableStateOf<SurpriseState>(SurpriseState.Loading) }

    LaunchedEffect(phase) {
        when (phase) {
            TimerPhase.Running -> {
                while (remaining > 0) {
                    delay(1000)
                    remaining -= 1
                }
                phase = TimerPhase.Finished
            }
            TimerPhase.Finished -> {
                surprise = SurpriseState.Loading
                surprise = fetchRandomDog()
            }
            TimerPhase.Idle -> {  }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (phase) {
            TimerPhase.Idle -> {
                Text("Set a timer", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = { seconds = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Sec") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = {
                    val total = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
                    if (total > 0) {
                        remaining = total
                        phase = TimerPhase.Running
                    }
                }) { Text("Start timer") }
            }

            TimerPhase.Running -> {
                Text("Time remaining", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "%02d:%02d".format(remaining / 60, remaining % 60),
                    style = MaterialTheme.typography.displayMedium,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = { phase = TimerPhase.Idle }) { Text("Cancel") }
            }

            TimerPhase.Finished -> {
                Text("Timer done. Here is a dog:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                when (val s = surprise) {
                    is SurpriseState.Loading -> CircularProgressIndicator()
                    is SurpriseState.Loaded -> {
                        Text(s.breed, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        Image(
                            bitmap = s.image,
                            contentDescription = s.breed,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is SurpriseState.Failed -> Text("Couldn't load a dog: ${s.message}")
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { phase = TimerPhase.Idle }) { Text("Reset") }
            }
        }
    }
}

private suspend fun fetchRandomDog(): SurpriseState = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    try {
        val imgJson = httpGetString(client, "https://dog.ceo/api/breeds/image/random")
        val imageUrl = JSONObject(imgJson).getString("message")
        val breed = breedFromUrl(imageUrl)
        val bitmap = downloadBitmap(client, imageUrl)
        SurpriseState.Loaded(breed, bitmap)
    } catch (e: Exception) {
        SurpriseState.Failed(e.message ?: e.javaClass.simpleName)
    }
}

private fun breedFromUrl(url: String): String {
    val slug = url.substringAfter("/breeds/", "").substringBefore("/")
    if (slug.isBlank()) return "Dog"
    return slug.split("-").reversed()
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

private fun httpGetString(client: OkHttpClient, url: String): String {
    val req = Request.Builder()
        .url(url)
        .header("User-Agent", "CPEN321-M1/1.0")
        .header("Accept", "application/json")
        .build()
    return client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) throw Exception("HTTP ${resp.code} from $url")
        resp.body?.string() ?: throw Exception("Empty response from $url")
    }
}

private fun downloadBitmap(client: OkHttpClient, url: String): ImageBitmap {
    val req = Request.Builder().url(url).header("User-Agent", "CPEN321-M1/1.0").build()
    return client.newCall(req).execute().use { resp ->
        val bytes = resp.body?.bytes() ?: throw Exception("No image data")
        (BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw Exception("Could not decode image")).asImageBitmap()
    }
}