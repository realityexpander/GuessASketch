package com.realityexpander.guessasketch.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.realityexpander.guessasketch.ui.common.Constants.DEFAULT_PAINT_STROKE_WIDTH
import java.util.*
import kotlin.math.abs

// @JvmOverloads creates the multiple various "overloaded" constructors for the class (for java interoperability)
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

    var isCanvasDrawing = false

    private var paint = Paint(Paint.DITHER_FLAG).apply {
        isDither = true
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = DEFAULT_PAINT_STROKE_WIDTH
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
        return curX ?: 0.0f
    }

    fun getCurrentY(): Float {
        return curY ?: 0.0f
    }

    fun getViewHeight(): Int {
        return viewHeight!!
    }

    fun getViewWidth(): Int {
        return viewWidth!!
    }


    data class PathData(val path: Path, val color: Int, val thickness: Float)

    private var path = Path() // current path being drawn
    private var pathDataStack = Stack<PathData>() // stack of all paths drawn

    // Called when the Stack of Paths has changed from touches (to send pathData to the server)
    private var pathDataStackChangedListener: ( (Stack<PathData>) -> Unit)? = null
    fun setPathDataStackChangedListener(listener: ( (Stack<PathData>) -> Unit) ) {
        pathDataStackChangedListener = listener
    }

    // Called after a config change to restore the pathDataStack
    fun restorePathDataStack(pathDataStack: Stack<PathData>) {
        this.pathDataStack = pathDataStack
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
        if (pathDataStack.size == 0 && !isCanvasDrawing) {
            canvas?.drawColor(Color.WHITE)
           return
        }

        // Draw the previous paths
        for(prevPath in pathDataStack) {
            paint.apply {
                color = prevPath.color
                strokeWidth = prevPath.thickness
                //canvas?.drawPath(pathData.path, paint)
            }
            canvas?.drawPath(prevPath.path, paint)
        }

        // Draw the current path the player is drawing (active path)
        if (isCanvasDrawing) {
            paint.apply {
                color = initialColor
                strokeWidth = initialThickness
            }
            canvas?.drawPath(path, paint)
        }

    }


    //////////////////////////////////////////////////////////
    //// DRAWING METHODS                                  ////
    //////////////////////////////////////////////////////////

    var isCanvasStartedTouch = false  // fixes Android bug where ACTION_MOVE is called after ACTION_UP

    private fun startTouch(x: Float, y: Float) {
        path = Path()  // Start a new path at the current point
        path.moveTo(x, y)

        curX = x
        curY = y

        isCanvasStartedTouch = true
        invalidate() // trigger onDraw()
    }

    private fun moveTouch(toX: Float, toY: Float) {
        if(!isCanvasStartedTouch) return

        val currX = curX ?: return
        val currY = curY ?: return

        val dx = abs(toX - currX)
        val dy = abs(toY - currY)
        if(dx >= smoothness || dy >= smoothness) {
            path.quadTo(currX, currY,
                (currX + toX) / 2f, (currY + toY) / 2f)

            curX = toX
            curY = toY

            isCanvasDrawing = true
            invalidate()
        }
    }

    private fun stopTouch() {
        val currX = curX ?: return
        val currY = curY ?: return
        if(!isCanvasStartedTouch) return

        path.lineTo(currX, currY)
        path.setLastPoint(currX, currY)

        // Add the path to the PathData stack
        pathDataStack.push(PathData(path, paint.color, paint.strokeWidth))
        pathDataStackChangedListener?.let { pathDataStackChanged ->
            pathDataStackChanged(pathDataStack)
        }

        isCanvasStartedTouch = false
        isCanvasDrawing = false
        invalidate()
    }

    fun clearDrawing() {
        canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY)
        pathDataStack.clear()
        path.reset()

        invalidate()
    }

    // Changing the enabled state of the view clears the drawing & path
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
    }

    fun undo() {
        if(pathDataStack.isNotEmpty()) {
            pathDataStack.pop()
            pathDataStackChangedListener?.let { pathDataStackChanged ->
                pathDataStackChanged(pathDataStack)
            }
        }

        invalidate()
    }


    //////////////////////////////////////////////////////////
    //// RESPOND TO THE SERVER SENDING A NEW PATH TO DRAW ////
    //////////////////////////////////////////////////////////

    private var isTouchStartedExternally = false

    fun startTouchExternally(fromX: Float, fromY: Float, color: Int, strokeWidth: Float) {
        isTouchStartedExternally = true

        paint.color = color
        paint.strokeWidth = strokeWidth
        startTouch(fromX, fromY)
    }

    fun moveTouchExternally(toX: Float, toY: Float, color: Int, strokeWidth: Float) {

        // Prevents a bug if startTouchExternally() is NOT called before moveTouchExternally()
        if (!isTouchStartedExternally) {
            startTouchExternally(toX, toY, color, strokeWidth)
            isTouchStartedExternally = true
        }

        paint.color = color
        paint.strokeWidth = strokeWidth
        moveTouch(toX, toY)
    }

    fun stopTouchExternally() {

        stopTouch()

        isTouchStartedExternally = false
    }

}









































