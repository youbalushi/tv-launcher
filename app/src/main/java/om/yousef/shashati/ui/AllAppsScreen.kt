package om.yousef.shashati.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import om.yousef.shashati.data.AppsRepository

/** Every app on the TV, opened from the "جميع التطبيقات" card. */
@Composable
fun AllAppsScreen(onLaunch: (String) -> Unit) {
    val context = LocalContext.current
    val apps = remember {
        AppsRepository.installed(context).map { a ->
            Triple(a, a.banner?.let { runCatching { it.toBitmap(320, 180).asImageBitmap() }.getOrNull() },
                a.icon?.let { runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull() })
        }
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        HomeBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 38.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineGlyph(Glyph.Grid, Modifier.size(20.dp), C.gold)
                Spacer(Modifier.width(10.dp))
                Text("جميع التطبيقات", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                Text("${apps.size} تطبيق", fontSize = 13.sp, color = C.dim)
            }
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(8.dp),
            ) {
                itemsIndexed(apps, key = { _, t -> t.first.packageName }) { i, (app, banner, icon) ->
                    GlowTile(
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        focusRequester = if (i == 0) first else null,
                        background = Brush.linearGradient(listOf(Color(0x40A6BEE6), Color(0x2A27467A))),
                        onClick = { onLaunch(app.packageName) }
                    ) {
                        if (banner != null) {
                            Image(banner, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                icon?.let { Image(it, null, Modifier.size(34.dp)) }
                                Spacer(Modifier.height(4.dp))
                                Text(app.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
