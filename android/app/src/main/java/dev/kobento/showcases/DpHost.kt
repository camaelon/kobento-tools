package dev.kobento.showcases

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlin.math.roundToInt

/**
 * Hosts one child laid out in dp: the child gets this view's size divided by the display density
 * and is scaled back up by it. A document without a density header (densityBehavior, API level 8)
 * is laid out by the androidx player in pixels, while the showcases are written in dp, as the web
 * and C++ players read them: without this a phone screen would be a 1080-unit-wide document and
 * everything in it a third of its size. View scaling keeps the drawing sharp (it scales the
 * recorded drawing, not a bitmap) and maps touches back into the child's coordinates.
 */
@SuppressLint("ViewConstructor")   // made in code, around its child: never inflated
class DpHost(context: Context, val child: View) : ViewGroup(context) {
    private val density = if (enabled) context.resources.displayMetrics.density else 1f

    companion object {
        /** Off (`--ez dp false` at launch): the player lays the document out in pixels, as it would by itself. */
        var enabled = true
    }

    init {
        clipChildren = true
        addView(child)
        child.pivotX = 0f
        child.pivotY = 0f
        child.scaleX = density
        child.scaleY = density
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(w, h)
        child.measure(
            MeasureSpec.makeMeasureSpec((w / density).roundToInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((h / density).roundToInt(), MeasureSpec.EXACTLY),
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        child.layout(0, 0, child.measuredWidth, child.measuredHeight)
    }
}
