package com.realityexpander.guessasketch.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.realityexpander.guessasketch.util.Constants
import java.util.*
import kotlin.math.abs

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
): View(context, attrs) {

    private var viewWidth: Int? = null
    private var viewHeight: Int? = null
    private var bmp: Bitmap? = null
    private var canvas: Canvas? = null
    private var curX: Float? = null
    private var curY: Float? = null
    private var smoothness = 5

    var isDrawing = false

    private var paint = Paint(Paint.DITHER_FLAG).apply {
        isDither = true
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = Constants.DEFAULT_PAINT_STROKE_WIDTH
    }

    fun setThickness(thickness: Float) {
        paint.strokeWidth = thickness
    }

    fun setColor(color: Int) {
        paint.color = color
    }

    data class PathData(val path: Path, val color: Int, val thickness: Float)

    private var path = Path()
    private var paths = Stack<PathData>()
    private var pathDataChangedListener: ( (Stack<PathData>) -> Unit)? = null

    fun setPathDataChangedListener(listener: ( (Stack<PathData>) -> Unit) ) {
        pathDataChangedListener = listener
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        if(!isEnabled) return false

        val newX = event.x
        val newY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> startTouch(newX, newY)
            MotionEvent.ACTION_MOVE -> moveTouch(newX, newY)
            MotionEvent.ACTION_UP -> stopTouch()
            MotionEvent.ACTION_CANCEL -> isDrawing = false
        }

        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bmp!!)
        canvas?.drawColor(Color.WHITE)
    }

    // Called many times per second
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val initialColor = paint.color
        val initialThickness = paint.strokeWidth

        // Draw the previous paths
        for(pathData in paths) {
            paint.apply {
                color = pathData.color
                strokeWidth = pathData.thickness
                canvas?.drawPath(pathData.path, paint)
            }
            canvas?.drawPath(pathData.path, paint)
        }

        // Draw the current path the player is drawing (active path)
        paint.apply {
            color = initialColor
            strokeWidth = initialThickness
        }
        canvas?.drawPath(path, paint)
    }

    private fun startTouch(x: Float, y: Float) {
        path = Path()  // Start a new path at the current point
        path.moveTo(x, y)

        curX = x
        curY = y

        invalidate() // trigger onDraw()
    }

    private fun moveTouch(toX: Float, toY: Float) {
        val currX = curX ?: return
        val currY = curY ?: return

        val dx = abs(toX - currX)
        val dy = abs(toY - currY)
        if(dx >= smoothness || dy >= smoothness) {
            isDrawing = true
            path.quadTo(currX, currY,
                (currX + toX) / 2f, (currY + toY) / 2f)

            curX = toX
            curY = toY

            invalidate()
        }
    }

    private fun stopTouch() {
        curX ?: return
        curY ?: return

        isDrawing = false
        path.lineTo(curX!!, curY!!)

        paths.push(PathData(path, paint.color, paint.strokeWidth))
        pathDataChangedListener?.let { pathDataChanged ->
            pathDataChanged(paths)
        }

        invalidate()
    }

    fun clearDrawing() {
        canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY)
        paths.clear()

        invalidate()
    }

}









































