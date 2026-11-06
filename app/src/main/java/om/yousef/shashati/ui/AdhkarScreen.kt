package om.yousef.shashati.ui

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import om.yousef.shashati.data.Adhkar

/** Morning or evening adhkar, one at a time, with a counter. */
@Composable
fun AdhkarScreen(morning: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val list = if (morning) Adhkar.morning else Adhkar.evening
    var index by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(list[0].times) }
    BackHandler { onBack() }

    fun go(i: Int) {
        index = i.coerceIn(0, list.size - 1)
        left = list[index].times
    }

    val countFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { countFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Focusable(modifier = Modifier.size(34.dp), shape = CircleShape, onClick = onBack) {
                        Text("›", fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
                    }
                    Text(if (morning) "أذكار الصباح" else "أذكار المساء", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("${index + 1} / ${list.size}", fontSize = 16.sp, color = C.dim)
            }

            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .background(Color(0x12FFFFFF), RoundedCornerShape(22.dp))
                    .padding(horizontal = 36.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    list[index].text,
                    fontFamily = Amiri, fontSize = if (list[index].text.length > 400) 21.sp else 26.sp,
                    lineHeight = if (list[index].text.length > 400) 38.sp else 46.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Focusable(
                    modifier = Modifier.height(48.dp).width(96.dp),
                    shape = RoundedCornerShape(50),
                    onClick = { go(index - 1) }
                ) { Text("السابق", fontSize = 14.sp, modifier = Modifier.align(Alignment.Center)) }

                Focusable(
                    modifier = Modifier.height(48.dp).width(200.dp),
                    shape = RoundedCornerShape(50),
                    focusRequester = countFocus,
                    background = C.gold,
                    onClick = {
                        if (left > 1) left-- else if (index < list.size - 1) go(index + 1) else left = 0
                    }
                ) {
                    Text(
                        if (left == 0) "انتهت الأذكار ✓" else if (list[index].times > 1) "اضغط OK · بقي $left" else "التالي (OK)",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.dark,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Focusable(
                    modifier = Modifier.height(48.dp).width(96.dp),
                    shape = RoundedCornerShape(50),
                    onClick = { go(index + 1) }
                ) { Text("التالي", fontSize = 14.sp, modifier = Modifier.align(Alignment.Center)) }

                Box(Modifier.weight(1f))

                Focusable(
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(50),
                    background = Color(0x1FFFFFFF),
                    onClick = { openAlafasy(context, morning) }
                ) {
                    Text(
                        "استماع بصوت العفاسي (يوتيوب)", fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

/** Opens Mishary Alafasy's adhkar recording in the YouTube app (needs internet). */
private fun openAlafasy(context: android.content.Context, morning: Boolean) {
    val query = if (morning) "أذكار الصباح مشاري العفاسي" else "أذكار المساء مشاري العفاسي"
    val search = Intent(Intent.ACTION_SEARCH)
        .setPackage("com.google.android.youtube.tv")
        .putExtra(SearchManager.QUERY, query)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val ok = runCatching { context.startActivity(search); true }.getOrDefault(false)
    if (!ok) {
        val url = "https://www.youtube.com/results?search_query=" + Uri.encode(query)
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
