package com.example.videoapp

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginBottom
import com.example.videoapp.network.PollOptionResponse
import com.squareup.picasso.Picasso
import kotlin.math.max

class PollItem  : ConstraintLayout{

    private lateinit var imageView : ImageView
    private lateinit var title :TextView
    private lateinit var percentage :TextView
    private var isSelected = false
    private lateinit var rect: RectF
    private var paintSelected = Paint().apply { color = ContextCompat.getColor(context, R.color.dark_blue); style = Paint.Style.FILL }
    private var paintNotSelected = Paint().apply { color = ContextCompat.getColor(context, R.color.dark_grey); style = Paint.Style.FILL }
    private lateinit var poll:Poll
    lateinit var option:PollOptionResponse
    private var percentageNumber = 0f
    private var w = 0f
    private var h = 0f
    private var imgW = 0f

    constructor(context: Context) : this(context, null) {
        setWillNotDraw(false)
    }
    constructor(context: Context, attrs:AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr:Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, option:PollOptionResponse, parent:Poll, isSelected:Boolean):this(context) {
        inflate(context, R.layout.poll_item, this)
        this.poll = parent
        imageView = findViewById(R.id.image)
        this.title = findViewById(R.id.title)
        this.percentage = findViewById(R.id.percentage)
        this.option = option
        rect = RectF(0f,0f,0f,0f)

        this.isSelected = isSelected
        this.title.text = option.name
        setOnClickListener {
            this.isSelected = this.isSelected.not()
            if(this.isSelected){
                poll.selectedChild(option.id)
            } else {
                poll.selectedChild(null)
            }
        }

        doOnLayout {
            val IMG_MAX_WIDTH_DP = 115
            imgW = IMG_MAX_WIDTH_DP * resources.displayMetrics.density
            Picasso.get().load("${MyApp.BASE_URL}/files/${option.resource}.jpg").resize((90*resources.displayMetrics.density).toInt(),0).into(imageView)
            h = height.toFloat()
            w = width.toFloat()
            val right = imgW + (width - imgW) * option.percentage / 100
            rect = RectF(7f, 5f, right, height.toFloat()-5)
        }
        checkState()
    }
    var imageUri:Uri? = null
    lateinit var content:String
    constructor(context: Context, content:String, image: Uri?):this(context) {
        inflate(context, R.layout.poll_item, this)
        imageView = findViewById(R.id.image)
        title = findViewById(R.id.title)
        percentage = findViewById(R.id.percentage)
        percentage.visibility = View.GONE
        this.content = content
        title.text = content
        Picasso.get().load(image).into(imageView)
        setOnLongClickListener {
            AlertDialog.Builder(context).apply {
                setMessage(R.string.remove_poll_item_question)
                setPositiveButton(R.string.remove){_,_->
                    (parent as Poll).removePollItem(this@PollItem)
                }
                setNegativeButton(R.string.cancel){_,_->}
                show()
            }
            true
        }
    }


    private fun checkState(){
        if(poll.hasVoted){
            calculatePercentage()
            percentage.text = context.getString(R.string.percentage, (percentageNumber * 100).toInt())
        } else {
            percentage.text = ""
        }

        if(isSelected){
            title.setTextColor(context.resources.getColor(R.color.dark_blue, context.theme))
            percentage.setTextColor(context.resources.getColor(R.color.dark_blue, context.theme))

            var drawable =AppCompatResources.getDrawable(context, R.drawable.custom_border_blue)
            background = drawable

        } else {
            title.setTextColor(context.resources.getColor(R.color.dark_grey, context.theme))
            percentage.setTextColor(context.resources.getColor(R.color.dark_grey, context.theme))

            var drawable =AppCompatResources.getDrawable(context, R.drawable.custom_border_grey)
            background = drawable
        }
        invalidate()
    }

    fun childChanged(childId:Int?){
        isSelected = option.id == childId
        checkState()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if(poll.hasVoted){
            canvas.drawRoundRect(rect,20f,20f, if(isSelected) paintSelected else paintNotSelected)
        } else {
            canvas.drawRoundRect(0f,0f,imgW, h,20f,20f, paintNotSelected)
        }
    }

    private fun calculatePercentage(){
        percentageNumber = option.votes.toFloat()
        if(option.ordinal == poll.poll.votedOrdinal && isSelected.not())
            percentageNumber = max(0f,percentageNumber-1)
        else if(option.ordinal != poll.poll.votedOrdinal && isSelected)
            percentageNumber++

        percentageNumber /= poll.poll.votes

        rect.right = imgW + (w - imgW) * percentageNumber
    }
}