package om.yousef.shashati.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private enum class Net { WIFI, ETHERNET, NONE }

private fun current(cm: ConnectivityManager?): Net {
    cm ?: return Net.WIFI
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return Net.NONE
    if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return Net.NONE
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Net.ETHERNET
        else -> Net.WIFI
    }
}

/** Wi-Fi icon that updates live; when there is no connection it is shown crossed out in red. */
@Composable
fun NetworkIndicator(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    var state by remember { mutableStateOf(current(cm)) }
    DisposableEffect(Unit) {
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { state = current(cm) }
            override fun onLost(network: Network) { state = current(cm) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { state = current(cm) }
        }
        runCatching { cm?.registerDefaultNetworkCallback(cb) }
        onDispose { runCatching { cm?.unregisterNetworkCallback(cb) } }
    }

    val offline = state == Net.NONE
    val color = if (offline) Color(0xFFFF8A8A) else Color.White
    Canvas(modifier.size(20.dp)) {
        val s = size.width
        val w = s * 0.09f
        val center = Offset(s / 2, s * 0.86f)
        if (state == Net.ETHERNET) {
            // Simple wired-network glyph: three boxes joined by lines
            val box = Size(s * 0.26f, s * 0.22f)
            drawRect(color, Offset(s * 0.37f, s * 0.08f), box, style = Stroke(w))
            drawRect(color, Offset(s * 0.08f, s * 0.68f), box, style = Stroke(w))
            drawRect(color, Offset(s * 0.66f, s * 0.68f), box, style = Stroke(w))
            drawLine(color, Offset(s * 0.5f, s * 0.3f), Offset(s * 0.5f, s * 0.5f), w)
            drawLine(color, Offset(s * 0.21f, s * 0.5f), Offset(s * 0.79f, s * 0.5f), w)
            drawLine(color, Offset(s * 0.21f, s * 0.5f), Offset(s * 0.21f, s * 0.68f), w)
            drawLine(color, Offset(s * 0.79f, s * 0.5f), Offset(s * 0.79f, s * 0.68f), w)
            return@Canvas
        }
        // Wi-Fi arcs
        for ((i, r) in listOf(0.78f, 0.52f, 0.27f).withIndex()) {
            val rad = s * r
            drawArc(
                color.copy(alpha = if (offline) 0.75f else 1f), 225f, 90f, false,
                topLeft = Offset(center.x - rad, center.y - rad), size = Size(rad * 2, rad * 2),
                style = Stroke(w, cap = StrokeCap.Round)
            )
            if (i == 2) drawCircle(color, s * 0.07f, center)
        }
        if (offline) {
            drawLine(Color(0xFF0A0F1C), Offset(s * 0.12f, s * 0.08f), Offset(s * 0.9f, s * 0.92f), w * 2.4f, StrokeCap.Round)
            drawLine(color, Offset(s * 0.12f, s * 0.08f), Offset(s * 0.9f, s * 0.92f), w, StrokeCap.Round)
        }
    }
}
