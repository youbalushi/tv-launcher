package om.yousef.shashati.ui

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import om.yousef.shashati.data.Channel

@OptIn(UnstableApi::class)
object Players {
    fun create(context: Context): ExoPlayer = ExoPlayer.Builder(context).build()

    fun source(channel: Channel, url: String): MediaSource {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Linux; Android TV) AppleWebKit/537.36 Chrome/120 Safari/537.36")
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
            .setDefaultRequestProperties(channel.headers)
        return HlsMediaSource.Factory(http).createMediaSource(MediaItem.fromUri(url))
    }

    /** Plays the channel's [urlIndex]-th address. */
    fun play(player: ExoPlayer, channel: Channel, urlIndex: Int) {
        val url = channel.urls.getOrNull(urlIndex) ?: return
        player.setMediaSource(source(channel, url))
        player.prepare()
        player.playWhenReady = true
    }
}
