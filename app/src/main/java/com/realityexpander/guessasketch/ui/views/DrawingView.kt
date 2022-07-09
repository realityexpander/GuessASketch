package com.realityexpander.guessasketch.ui.views

import android.annotation.SuppressLint
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

    fun setStrokeWidth(thickness: Float) {
        paint.strokeWidth = thickness
    }

    fun getStrokeWidth(): Float {
        return paint.strokeWidth
    }

    fun setColor(color: Int) {
        paint.color = color
    }

    fun getColor(): Int {
        return paint.color
    }

    fun getCurrentX(): Float {
        return curX!!
    }

    fun getCurrentY(): Float {
        return curY!!
    }

    fun getViewHeight(): Int {
        return viewHeight!!
    }

    fun getViewWidth(): Int {
        return viewWidth!!
    }


    data class PathData(val path: Path, val color: Int, val thickness: Float)

    private var path = Path()
    private var paths = Stack<PathData>()

    // Called when the Stack of Paths has changed (ie: from the server from another player)
    private var pathDataStackChangedListener: ( (Stack<PathData>) -> Unit)? = null
    fun setPathDataStackChangedListener(listener: ( (Stack<PathData>) -> Unit) ) {
        pathDataStackChangedListener = listener
    }

    // Respond to the user of the device drawing on the screen with finger or stylus
    @SuppressLint("ClickableViewAccessibility")  // for onTouchEvent not implementing performClick()
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        if(!isEnabled) return false  // touches are enabled only for the drawing player

        val newX = event.x
        val newY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> startTouch(newX, newY)
            MotionEvent.ACTION_MOVE -> moveTouch(newX, newY)
            MotionEvent.ACTION_UP -> stopTouch()
            //MotionEvent.ACTION_CANCEL -> stopTouch() //isDrawing = false
        }

        return true
    }

    // Called when inflating the view from XML, and on config changes
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        viewWidth = w
        viewHeight = h
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bmp!!)
        canvas?.drawColor(Color.WHITE)  // fill the canvas with white (very poor naming of the method!!!)
    }

    // Called many times per second
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // Save the current drawing paint options
        val initialColor = paint.color
        val initialThickness = paint.strokeWidth

        // Fresh canvas
        if (paths.size == 0 && !isDrawing) {
            canvas?.drawColor(Color.WHITE)
           return
        }

        // Draw the previous paths
        for(prevPath in paths) {
            paint.apply {
                color = prevPath.color
                strokeWidth = prevPath.thickness
                //canvas?.drawPath(pathData.path, paint)
            }
            canvas?.drawPath(prevPath.path, paint)
        }

        // Draw the current path the player is drawing (active path)
        if (isDrawing) {
            paint.apply {
                color = initialColor
                strokeWidth = initialThickness
            }
            canvas?.drawPath(path, paint)
        }

    }

    //// DRAWING METHODS ////

    var isStarted = false  // fixes Android bug where ACTION_MOVE is called after ACTION_UP

    private fun startTouch(x: Float, y: Float) {
        path = Path()  // Start a new path at the current point
        path.moveTo(x, y)

        curX = x
        curY = y

        isStarted = true
        invalidate() // trigger onDraw()
    }

    private fun moveTouch(toX: Float, toY: Float) {
        if(!isStarted) return

        val currX = curX ?: return
        val currY = curY ?: return

        val dx = abs(toX - currX)
        val dy = abs(toY - currY)
        if(dx >= smoothness || dy >= smoothness) {
            path.quadTo(currX, currY,
                (currX + toX) / 2f, (currY + toY) / 2f)

            curX = toX
            curY = toY

            isDrawing = true
            invalidate()
        }
    }

    private fun stopTouch() {
        curX ?: return
        curY ?: return
        if(!isStarted) return

        path.lineTo(curX!!, curY!!)         // todo remove the null checks
        path.setLastPoint(curX!!, curY!!)

        // Add the path to the stack
        paths.push(PathData(path, paint.color, paint.strokeWidth))
        pathDataStackChangedListener?.let { pathDataChanged ->
            pathDataChanged(paths)
        }

        isDrawing = false
        isStarted = false
        invalidate()
    }

    fun clearDrawing() {
        canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY)
        paths.clear()
        path.reset()

        invalidate()
    }

    // Changing the enabled state of the view clears the drawing & path
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        clearDrawing()
    }

    fun undo() {
        if(paths.isNotEmpty()) {
            paths.pop()
            pathDataStackChangedListener?.let { pathDataStackChanged ->
                pathDataStackChanged(paths)
            }
        }

        invalidate()
    }

    //////////////////////////////////////////////////////////
    //// RESPOND TO THE SERVER SENDING A NEW PATH TO DRAW ////

    private var startedTouchExternally = false

    fun startTouchExternally(fromX: Float, fromY: Float, color: Int, strokeWidth: Float) {
        startedTouchExternally = true

        paint.color = color
        paint.strokeWidth = strokeWidth
        startTouch(fromX, fromY)
    }

    fun moveTouchExternally(toX: Float, toY: Float, color: Int, strokeWidth: Float) {

        // Prevents a bug if startTouchExternally() is NOT called before moveTouchExternally()
        if (!startedTouchExternally) {
            startTouchExternally(toX, toY, color, strokeWidth)
            startedTouchExternally = true
        }

        paint.color = color
        paint.strokeWidth = strokeWidth
        moveTouch(toX, toY)
    }

    fun stopTouchExternally() {

        stopTouch()

        startedTouchExternally = false
    }

}









































