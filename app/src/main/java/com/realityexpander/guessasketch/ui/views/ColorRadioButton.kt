package com.realityexpander.guessasketch.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import com.realityexpander.guessasketch.R
import kotlin.math.min
import kotlin.properties.Delegates

// @JvmOverloads creates the multiple various "overloaded" constructors for the class (for java interoperability)
class ColorRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,  // AttributeSet is a class that contains the attributes of the XML file
) : AppCompatRadioButton(context, attrs) {
    // private lateinit var buttonColor: Int                   // cant use lateinit var for primitives...
    private var buttonColor by Delegates.notNull<Int>()   // so use Delegates.notNull to make it nullable
    private var buttonRadius = 25f

    private var viewWidth by Delegates.notNull<Int>()
    private var viewHeight by Delegates.notNull<Int>()

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ColorRadioButton,
            0,
            0
        ).apply {
            try {
                buttonColor = getColor(R.styleable.ColorRadioButton_buttonColor, Color.BLACK)
                buttonRadius = getDimension(R.styleable.ColorRadioButton_buttonRadius, 25f)
            } finally {
                recycle()
            }

            buttonPaint.apply {
                color = buttonColor
                style = Paint.Style.FILL
            }
            selectionPaint.apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = buttonRadius / 2
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        viewWidth = w
        viewHeight = h
        buttonRadius = min(w, h) / 2 * 0.8f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawCircle(
            viewWidth / 2f,
            viewHeight / 2f,
            buttonRadius,
            buttonPaint
        )

        if(isChecked) {
            canvas.drawCircle(
                viewWidth / 2f,
                viewHeight / 2f,
                buttonRadius * 1.1f,
                selectionPaint
            )
        }
    }
}



































