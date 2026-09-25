package com.example.expensetrack

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class PieSlice(
        val category: String,
        val value: Double,
        val color: Int
    )

    private val slices = mutableListOf<PieSlice>()
    private var totalValue = 0.0
    private var animationProgress = 1f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val rectF = RectF()

    fun setData(newSlices: List<PieSlice>, animate: Boolean = true) {
        slices.clear()
        slices.addAll(newSlices.filter { it.value > 0 })
        totalValue = slices.sumOf { it.value }

        if (animate) {
            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 800
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    animationProgress = it.animatedValue as Float
                    invalidate()
                }
            }
            animator.start()
        } else {
            animationProgress = 1f
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height)
        val padding = 20f
        val diameter = size - padding * 2
        if (diameter <= 0) return

        val left = (width - diameter) / 2f
        val top = (height - diameter) / 2f
        rectF.set(left, top, left + diameter, top + diameter)

        if (slices.isEmpty() || totalValue <= 0) {
            // Draw empty state donut
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawArc(rectF, 0f, 360f, true, paint)

            val holeRadius = diameter * 0.35f
            val centerX = width / 2f
            val centerY = height / 2f
            canvas.drawCircle(centerX, centerY, holeRadius, holePaint)

            canvas.drawText("No Data", centerX, centerY + 8f, textPaint)
            return
        }

        var startAngle = -90f
        val totalSweep = 360f * animationProgress

        for (slice in slices) {
            val sweepAngle = ((slice.value / totalValue) * totalSweep).toFloat()
            paint.color = slice.color
            
            // Draw slice arc with slight gap separator if multiple slices
            if (slices.size > 1 && sweepAngle > 2f) {
                canvas.drawArc(rectF, startAngle + 1f, sweepAngle - 2f, true, paint)
            } else {
                canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
            }

            startAngle += sweepAngle
        }

        // Draw inner donut hole
        val holeRadius = diameter * 0.33f
        val centerX = width / 2f
        val centerY = height / 2f
        canvas.drawCircle(centerX, centerY, holeRadius, holePaint)

        // Text inside donut
        canvas.drawText("Total", centerX, centerY - 10f, subTextPaint)
        canvas.drawText(Utils.formatCurrency(totalValue), centerX, centerY + 30f, textPaint)
    }
}
