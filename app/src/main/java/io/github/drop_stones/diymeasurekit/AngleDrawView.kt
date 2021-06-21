package io.github.drop_stones.diymeasurekit

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.tan

class AngleDrawView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint: Paint = Paint()
    private lateinit var canvas: Canvas

    private val leftTriangle: Path = Path()
    private val rightTriangle: Path = Path()

    private var theta: Float = 0F

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        this.canvas = canvas

        val w: Float = width.toFloat()
        val h: Float = height.toFloat()
        //paint.color = Color.rgb(0x40, 0x40, 0x40)
        paint.color = Color.rgb(0xB3, 0xB3, 0xB3)

        val leftY: Float = h / 2 + tan(theta) * (w / 2)
        val rightY: Float = h / 2 - tan(theta) * (w / 2)

        leftTriangle.reset()
        leftTriangle.fillType = Path.FillType.EVEN_ODD
        leftTriangle.moveTo(w / 2, h / 2)
        leftTriangle.lineTo(0F, h / 2)
        leftTriangle.lineTo(0F, leftY)
        leftTriangle.close()

        rightTriangle.reset()
        rightTriangle.fillType = Path.FillType.EVEN_ODD
        rightTriangle.moveTo(w / 2, h / 2)
        rightTriangle.lineTo(w, h / 2)
        rightTriangle.lineTo(w, rightY)
        rightTriangle.close()

        canvas.drawPath(leftTriangle, paint)
        canvas.drawPath(rightTriangle, paint)
    }

    fun setTheta(theta: Float) {
        this.theta = theta
        invalidate()
    }
}