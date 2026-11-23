package om.yousef.shashati.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import om.yousef.shashati.data.Channel
import om.yousef.shashati.data.Logos

/**
 * Vertical channel list shown over the playing video (left side, see-through background).
 * Driven entirely by the player's key handling: [selected] is the highlighted row.
 */
@Composable
fun ChannelPanel(
    tabs: List<String>,
    tab: Int,
    list: List<Channel>,
    selected: Int,
    playingId: String,
    numbers: Map<String, Int>,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected, tab) {
        if (list.isNotEmpty()) listState.animateScrollToItem((selected - 2).coerceAtLeast(0))
    }
    val tabScroll = rememberScrollState()

    Box(
        Modifier.width(300.dp).fillMaxHeight()
            .background(Brush.horizontalGradient(listOf(Color(0xE0080E1E), Color(0xB8080E1E), Color(0x00080E1E)), 0f, Float.POSITIVE_INFINITY))
    ) {
        Column(Modifier.fillMaxHeight().padding(start = 18.dp, end = 24.dp, top = 22.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(17.dp).background(C.gold, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text("القنوات", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            LaunchedEffect(tab) { tabScroll.animateScrollTo((tab * 70).coerceAtMost(tabScroll.maxValue)) }
            Row(Modifier.horizontalScroll(tabScroll), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tabs.forEachIndexed { i, name ->
                    val on = i == tab
                    Text(
                        name,
                        Modifier
                            .background(if (on) Color.White else Color(0x1AFFFFFF), RoundedCornerShape(50))
                            .border(1.dp, if (on) Color.Transparent else Color(0x2EFFFFFF), RoundedCornerShape(50))
                            .padding(horizontal = 11.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (on) C.dark else Color.White
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (list.isEmpty()) {
                Text("لا توجد قنوات في هذا القسم", fontSize = 13.sp, color = C.dim)
            }
            LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(list, key = { _, c -> c.id }) { i, ch ->
                    PanelRow(ch, numbers[ch.id], focused = i == selected, playing = ch.id == playingId)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("▲▼ تنقّل  ·  OK تشغيل  ·  OK مرة ثانية أو رجوع إغلاق  ·  ◀ ▶ الأقسام", fontSize = 10.sp, color = C.faint)
        }
    }
}

@Composable
private fun PanelRow(ch: Channel, number: Int?, focused: Boolean, playing: Boolean) {
    val context = LocalContext.current
    var logo by remember(ch.id) { mutableStateOf(Logos.cachedChannel(context, ch)) }
    LaunchedEffect(ch.id) { if (logo == null) logo = Logos.channel(context, ch) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .fillMaxWidth()
            .height(46.dp)
            .scale(if (focused) 1.03f else 1f)
            .then(if (focused) Modifier.outerGlow(shape, Color(0xFFFFB84A), 7.dp) else Modifier)
            .background(
                when {
                    focused -> Color(0x29FFFFFF)
                    playing -> Color(0x29E2B45A)
                    else -> Color(0x12FFFFFF)
                }, shape
            )
            .border(
                if (focused) 2.dp else 1.dp,
                when {
                    focused -> Color(0xFFFFD27A)
                    playing -> Color(0x80E2B45A)
                    else -> Color(0x1FFFFFFF)
                }, shape
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${number ?: ""}", fontSize = 11.sp, color = C.faint, modifier = Modifier.width(22.dp))
        Box(Modifier.size(58.dp, 30.dp), contentAlignment = Alignment.Center) {
            val l = logo
            if (l != null) Image(l, null, Modifier.size(58.dp, 28.dp), contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            ch.name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        if (playing) Text("● الآن", fontSize = 10.sp, color = C.gold)
    }
}
