package com.realityexpander.guessasketch.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.realityexpander.guessasketch.util.Constants
import java.util.*

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

    var smoothness = 5
    var isDrawingView = false

    private var paint = Paint(Paint.DITHER_FLAG).apply {
        isDither = true
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = Constants.DEFAULT_PAINT_STROKE_WIDTH
    }

    private var path = Path()
    private var paths = Stack<PathData>()
    private var pathDataChangedListener: ( (Stack<PathData>) -> Unit)? = null

    fun setPathDataChangedListener(listener: ( (Stack<PathData>) -> Unit) ) {
        pathDataChangedListener = listener
    }

    data class PathData(val path: Path, val color: Int, val thickness: Float)

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

}