package com.example.cpen321application

import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

private const val GRID = 16

@Composable
fun LiveUpdatesScreen(modifier: Modifier = Modifier) {
    // 16x16 cells, initialised white (blank canvas).
    val cells = remember {
        mutableStateListOf<Int>().apply { repeat(GRID * GRID) { add(AndroidColor.WHITE) } }
    }
    var status by remember { mutableStateOf("Connecting…") }

    DisposableEffect(Unit) {
        val main = Handler(Looper.getMainLooper())
        val wsUrl = BuildConfig.API_BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/pixels"

        val client = OkHttpClient()
        val ws = client.newWebSocket(
            Request.Builder().url(wsUrl).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    main.post { status = "Connected" }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val o = JSONObject(text)
                        val x = o.getInt("x")
                        val y = o.getInt("y")
                        val color = parseColorSafe(o.getString("color"))
                        if (x in 0 until GRID && y in 0 until GRID) {
                            main.post { cells[y * GRID + x] = color }
                        }
                    } catch (_: Exception) { /* ignore malformed */ }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    main.post { status = "Disconnected: ${t.message ?: "error"}" }
                }
            },
        )

        onDispose {
            ws.close(1000, null)
            client.dispatcher.executorService.shutdown()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(status)
        Spacer(Modifier.height(16.dp))
        PixelGrid(cells)
    }
}

@Composable
private fun PixelGrid(cells: List<Int>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val cell = size.minDimension / GRID
        for (i in 0 until GRID * GRID) {
            val cx = (i % GRID) * cell
            val cy = (i / GRID) * cell
            drawRect(
                color = Color(cells[i]),
                topLeft = Offset(cx, cy),
                size = Size(cell, cell),
            )
        }
        // Light grid lines so the blank canvas is visible.
        for (n in 0..GRID) {
            val p = n * cell
            drawLine(Color.LightGray, Offset(p, 0f), Offset(p, cell * GRID), 1f)
            drawLine(Color.LightGray, Offset(0f, p), Offset(cell * GRID, p), 1f)
        }
    }
}

private fun parseColorSafe(hex: String): Int {
    val s = hex.trim()
    val withHash = if (s.startsWith("#")) s else "#$s"
    return try {
        AndroidColor.parseColor(withHash)
    } catch (_: Exception) {
        AndroidColor.GRAY
    }
}