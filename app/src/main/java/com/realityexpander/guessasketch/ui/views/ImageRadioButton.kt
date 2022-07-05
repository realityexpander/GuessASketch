package com.realityexpander.guessasketch.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.realityexpander.guessasketch.R
import kotlin.math.min
import kotlin.properties.Delegates

// @JvmOverloads creates multiple constructors for the class (for java compatibility)
class ImageRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,  // AttributeSet is a class that contains the attributes of the XML file
) : AppCompatRadioButton(context, attrs) {

    private var uncheckedDrawable: VectorDrawableCompat? = null
    private var checkedDrawable: VectorDrawableCompat? = null

    private var viewWidth by Delegates.notNull<Int>()
    private var viewHeight by Delegates.notNull<Int>()

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ImageRadioButton,
            0,
            0
        ).apply {
            try {
                val uncheckedId = getResourceId(R.styleable.ImageRadioButton_uncheckedDrawable, 0)
                val checkedId = getResourceId(R.styleable.ImageRadioButton_checkedDrawable, 0)
                if(uncheckedId != 0) {
                    uncheckedDrawable = VectorDrawableCompat.create(context.resources, uncheckedId, null)
                }
                if(checkedId != 0) {
                    checkedDrawable = VectorDrawableCompat.create(context.resources, checkedId, null)
                }
            } finally {
                recycle()
            }


        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        viewWidth = w
        viewHeight = h
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let { canvas ->
            if(!isChecked) {
                uncheckedDrawable?.setBounds(
                    paddingLeft,
                    paddingTop,
                    viewWidth - paddingRight,
                    viewHeight - paddingBottom
                )
                uncheckedDrawable?.draw(canvas)
            } else {
                checkedDrawable?.setBounds(
                    paddingLeft,
                    paddingTop,
                    viewWidth - paddingRight,
                    viewHeight - paddingBottom
                )
                checkedDrawable?.draw(canvas)
            }
        }
    }
}



































