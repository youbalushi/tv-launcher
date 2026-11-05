package om.yousef.shashati.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import om.yousef.shashati.R
import om.yousef.shashati.data.Locations
import om.yousef.shashati.data.Prefs
import om.yousef.shashati.prayer.Dates
import om.yousef.shashati.prayer.PRAYER_NAMES
import java.time.LocalTime

/** A small silent card in a screen corner, drawn above any app. It never takes focus or sound. */
object OverlayNotifier {
    private const val GOLD = 0xFFE2B45A.toInt()
    private const val GREEN = 0xFF6FD3A8.toInt()

    fun canShow(context: Context) = Settings.canDrawOverlays(context)

    fun show(context: Context, index: Int, iqama: Boolean, adhanTime: LocalTime?, onDone: () -> Unit): Boolean {
        if (!canShow(context)) return false
        val wm = context.getSystemService(WindowManager::class.java) ?: return false
        val main = Handler(Looper.getMainLooper())
        main.post {
            runCatching { addCard(context, wm, index, iqama, adhanTime, main, onDone) }
                .onFailure { onDone() }
        }
        return true
    }

    private fun dp(context: Context, v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, context.resources.displayMetrics)

    private fun addCard(
        context: Context, wm: WindowManager, index: Int, iqama: Boolean,
        adhanTime: LocalTime?, main: Handler, onDone: () -> Unit,
    ) {
        val accent = if (iqama) GREEN else GOLD
        val name = PRAYER_NAMES[index]
        val title = if (iqama) "حان وقت إقامة صلاة $name" else "حان الآن موعد صلاة $name"
        val minutes = Prefs.iqamaMinutes[index]
        val now = LocalTime.now()
        val subtitle = if (iqama) "بعد $minutes دقيقة من الأذان · ${Dates.time12(now, true)}"
        else "${Locations.current().name} · ${Dates.time12(adhanTime ?: now, true)}"

        val semibold = ResourcesCompat.getFont(context, R.font.plex_semibold)
        val regular = ResourcesCompat.getFont(context, R.font.plex_regular)

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 14f)
                setColor(0xE60E121C.toInt())
                setStroke(dp(context, 1f).toInt(), 0x2EFFFFFF)
            }
            clipToOutline = true
            elevation = dp(context, 12f)
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val p = dp(context, 15f).toInt()
            setPadding(p, dp(context, 12f).toInt(), p, dp(context, 12f).toInt())
        }

        val iconBox = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(46, Color.red(accent), Color.green(accent), Color.blue(accent)))
            }
        }
        val icon = ImageView(context).apply {
            setImageResource(if (iqama) R.drawable.ic_clock else R.drawable.ic_moon)
            setColorFilter(accent)
        }
        val iconSize = dp(context, 36f).toInt()
        iconBox.addView(icon, FrameLayout.LayoutParams(dp(context, 19f).toInt(), dp(context, 19f).toInt(), Gravity.CENTER))
        row.addView(iconBox, LinearLayout.LayoutParams(iconSize, iconSize))

        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 12f).toInt(), 0, 0, 0)
        }
        texts.addView(TextView(context).apply {
            text = title
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            typeface = semibold
            textDirection = View.TEXT_DIRECTION_RTL
        })
        texts.addView(TextView(context).apply {
            text = subtitle
            setTextColor(0xB3FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f)
            typeface = regular
            textDirection = View.TEXT_DIRECTION_RTL
        })
        row.addView(texts)
        card.addView(row, LinearLayout.LayoutParams(dp(context, 300f).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT))

        val track = FrameLayout(context).apply { setBackgroundColor(0x1AFFFFFF) }
        val bar = View(context).apply { setBackgroundColor(accent) }
        track.addView(bar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        card.addView(track, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 2f).toInt()))

        val pos = Prefs.notifPos
        val vertical = if (pos < 2) Gravity.TOP else Gravity.BOTTOM
        val horizontal = if (pos % 2 == 0) Gravity.LEFT else Gravity.RIGHT
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = vertical or horizontal
            x = dp(context, 32f).toInt()
            y = dp(context, 28f).toInt()
        }

        card.alpha = 0f
        card.translationY = if (pos < 2) -dp(context, 8f) else dp(context, 8f)
        wm.addView(card, lp)
        card.animate().alpha(1f).translationY(0f).setDuration(400).start()

        val seconds = Prefs.notifSeconds.coerceIn(5, 30)
        bar.post {
            bar.pivotX = 0f
            ValueAnimator.ofFloat(1f, 0f).apply {
                duration = seconds * 1000L
                interpolator = LinearInterpolator()
                addUpdateListener { bar.scaleX = it.animatedValue as Float }
                start()
            }
        }

        main.postDelayed({
            card.animate().alpha(0f).setDuration(400).withEndAction {
                runCatching { wm.removeView(card) }
                onDone()
            }.start()
        }, seconds * 1000L)
    }
}
