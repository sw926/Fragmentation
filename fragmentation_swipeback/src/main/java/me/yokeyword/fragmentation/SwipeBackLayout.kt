package me.yokeyword.fragmentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentationMagician
import me.yokeyword.fragmentation_swipeback.R
import me.yokeyword.fragmentation_swipeback.core.ISwipeBackActivity
import timber.log.Timber
import java.util.*
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * Thx https://github.com/ikew0ng/SwipeBackLayout.
 *
 *
 * Created by YoKey on 16/4/19.
 */
@Suppress("unused")
class SwipeBackLayout @JvmOverloads constructor(private val mContext: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(mContext, attrs, defStyleAttr) {


    companion object {
        /**
         * Edge flag indicating that the left edge should be affected.
         */
        const val EDGE_LEFT = ViewDragHelper.EDGE_LEFT

        /**
         * Edge flag indicating that the right edge should be affected.
         */
        const val EDGE_RIGHT = ViewDragHelper.EDGE_RIGHT
        const val EDGE_ALL = EDGE_LEFT or EDGE_RIGHT

        /**
         * A view is not currently being dragged or animating as a result of a
         * fling/snap.
         */
        const val STATE_IDLE = ViewDragHelper.STATE_IDLE

        /**
         * A view is currently being dragged. The position is currently changing as
         * a result of user input or simulated user input.
         */
        const val STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING

        /**
         * A view is currently settling into place as a result of a fling or
         * predefined non-interactive motion.
         */
        const val STATE_SETTLING = ViewDragHelper.STATE_SETTLING

        /**
         * A view is currently drag finished.
         */
        const val STATE_FINISHED = 3
        private const val DEFAULT_SCRIM_COLOR = -0x67000000
        private const val DEFAULT_PARALLAX = 0.33f
        private const val FULL_ALPHA = 255
        private const val DEFAULT_SCROLL_THRESHOLD = 0.4f
        private const val OVERSCROLL_DISTANCE = 10
    }

    private var mScrollFinishThreshold = DEFAULT_SCROLL_THRESHOLD

    /**
     * Get ViewDragHelper
     */
    private val viewDragHelper: ViewDragHelper

    private var mScrollPercent = 0f
    private var mScrimOpacity = 0f
    private var mActivity: FragmentActivity? = null
    private var mContentView: View? = null
    private var mFragment: ISupportFragment? = null
    private var mPreFragment: Fragment? = null
    private var mShadowLeft: Drawable? = null
    private var mShadowRight: Drawable? = null
    private val mTmpRect = Rect()
    private var mEdgeFlag = 0
    private var mEnable = true
    private var mCurrentSwipeOrientation = 0
    private var mParallaxOffset = DEFAULT_PARALLAX
    private var mCallOnDestroyView = false
    private var mInLayout = false
    private var mContentLeft = 0
    private var mContentTop = 0
    private var mSwipeAlpha = 0.5f

    /**
     * The set of listeners to be sent events through.
     */
    private var mListeners: MutableList<OnSwipeListener>? = null

    enum class EdgeLevel {
        MAX, MIN, MED
    }


    init {
        viewDragHelper = ViewDragHelper.create(this, ViewDragCallback())
        setShadow(R.drawable.shadow_left, EDGE_LEFT)
        setEdgeOrientation(EDGE_LEFT)
    }

    /**
     * 滑动中，上一个页面View的阴影透明度
     *
     * @param alpha 0.0f:无阴影, 1.0f:较重的阴影, 默认:0.5f
     */
    fun setSwipeAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float) {
        mSwipeAlpha = alpha
    }

    /**
     * Set scroll threshold, we will close the activity, when scrollPercent over
     * this value
     *
     * @param threshold
     */
    fun setScrollThresHold(@FloatRange(from = 0.0, to = 1.0) threshold: Float) {
        require(!(threshold >= 1.0f || threshold <= 0)) { "Threshold value should be between 0 and 1.0" }
        mScrollFinishThreshold = threshold
    }

    fun setParallaxOffset(offset: Float) {
        mParallaxOffset = offset
    }

    /**
     * Enable edge tracking for the selected edges of the parent view.
     * The callback's [ViewDragHelper.Callback.onEdgeTouched] and
     * [ViewDragHelper.Callback.onEdgeDragStarted] methods will only be invoked
     * for edges for which edge tracking has been enabled.
     *
     * @param orientation Combination of edge flags describing the edges to watch
     * @see .EDGE_LEFT
     *
     * @see .EDGE_RIGHT
     */
    fun setEdgeOrientation(@EdgeOrientation orientation: Int) {
        mEdgeFlag = orientation
        viewDragHelper.setEdgeTrackingEnabled(orientation)
        if (orientation == EDGE_RIGHT || orientation == EDGE_ALL) {
            setShadow(R.drawable.shadow_right, EDGE_RIGHT)
        }
    }

    @IntDef(EDGE_LEFT, EDGE_RIGHT, EDGE_ALL)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class EdgeOrientation

    /**
     * Set a drawable used for edge shadow.
     */
    fun setShadow(shadow: Drawable?, edgeFlag: Int) {
        if (edgeFlag and EDGE_LEFT != 0) {
            mShadowLeft = shadow
        } else if (edgeFlag and EDGE_RIGHT != 0) {
            mShadowRight = shadow
        }
        invalidate()
    }

    /**
     * Set a drawable used for edge shadow.
     */
    fun setShadow(resId: Int, edgeFlag: Int) {
        setShadow(ContextCompat.getDrawable(context, resId), edgeFlag)
    }

    /**
     * Add a callback to be invoked when a swipe event is sent to this view.
     *
     * @param listener the swipe listener to attach to this view
     */
    fun addSwipeListener(listener: OnSwipeListener) {
        if (mListeners == null) {
            mListeners = ArrayList()
        }
        mListeners?.add(listener)
    }

    /**
     * Removes a listener from the set of listeners
     *
     * @param listener
     */
    fun removeSwipeListener(listener: OnSwipeListener) {
        if (mListeners == null) {
            return
        }
        mListeners?.remove(listener)
    }

    interface OnSwipeListener {
        /**
         * Invoke when state change
         *
         * @param state flag to describe scroll state
         * @see .STATE_IDLE
         *
         * @see .STATE_DRAGGING
         *
         * @see .STATE_SETTLING
         *
         * @see .STATE_FINISHED
         */
        fun onDragStateChange(state: Int)

        /**
         * Invoke when edge touched
         *
         * @param oritentationEdgeFlag edge flag describing the edge being touched
         * @see .EDGE_LEFT
         *
         * @see .EDGE_RIGHT
         */
        fun onEdgeTouch(oritentationEdgeFlag: Int)

        /**
         * Invoke when scroll percent over the threshold for the first time
         *
         * @param scrollPercent scroll percent of this view
         */
        fun onDragScrolled(scrollPercent: Float)
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val isDrawView = child === mContentView
        val drawChild = super.drawChild(canvas, child, drawingTime)
        if (isDrawView && mScrimOpacity > 0 && viewDragHelper.viewDragState != ViewDragHelper.STATE_IDLE) {
            drawShadow(canvas, child)
            drawScrim(canvas, child)
        }
        return drawChild
    }

    private fun drawShadow(canvas: Canvas, child: View) {
        val childRect = mTmpRect
        child.getHitRect(childRect)
        if (mCurrentSwipeOrientation and EDGE_LEFT != 0) {
            mShadowLeft?.let {
                it.setBounds(childRect.left - it.intrinsicWidth, childRect.top, childRect.left, childRect.bottom)
                it.alpha = (mScrimOpacity * FULL_ALPHA).toInt()
                it.draw(canvas)
            }
        } else if (mCurrentSwipeOrientation and EDGE_RIGHT != 0) {
            mShadowRight?.let {
                it.setBounds(childRect.right, childRect.top, childRect.right + it.intrinsicWidth, childRect.bottom)
                it.alpha = (mScrimOpacity * FULL_ALPHA).toInt()
                it.draw(canvas)
            }
        }
    }

    private fun drawScrim(canvas: Canvas, child: View) {
        val baseAlpha = DEFAULT_SCRIM_COLOR and -0x1000000 ushr 24
        val alpha = (baseAlpha * mScrimOpacity * mSwipeAlpha).toInt()
        val color = alpha shl 24
        if (mCurrentSwipeOrientation and EDGE_LEFT != 0) {
            canvas.clipRect(0, 0, child.left, height)
        } else if (mCurrentSwipeOrientation and EDGE_RIGHT != 0) {
            canvas.clipRect(child.right, 0, right, height)
        }
        canvas.drawColor(color)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        mInLayout = true
        mContentView?.let {
            it.layout(mContentLeft,
                    mContentTop,
                    mContentLeft + it.measuredWidth,
                    mContentTop + it.measuredHeight)
        }
        mInLayout = false
    }

    override fun requestLayout() {
        if (!mInLayout) {
            super.requestLayout()
        }
    }

    override fun computeScroll() {
        mScrimOpacity = 1 - mScrollPercent
        if (mScrimOpacity >= 0) {
            if (viewDragHelper.continueSettling(true)) {
                ViewCompat.postInvalidateOnAnimation(this)
            }
            mPreFragment?.view?.let { preFragmentView ->
                if (mCallOnDestroyView) {
                    preFragmentView.x = 0f
                    return
                }
                viewDragHelper.capturedView?.let {
                    val leftOffset = ((it.left - width) * mParallaxOffset * mScrimOpacity).toInt()
                    preFragmentView.x = if (leftOffset > 0) 0f else leftOffset.toFloat()
                }
            }
        }
    }

    fun isSwiped(): Boolean {
        return mScrollPercent > 1
    }

    /**
     * hide
     */
    fun internalCallOnDestroyView() {
        mCallOnDestroyView = true
    }

    fun setFragment(fragment: ISupportFragment?, view: View?) {
        mFragment = fragment
        mContentView = view
    }

    fun hiddenFragment() {
        mPreFragment?.view?.visibility = View.GONE
    }

    fun attachToActivity(activity: FragmentActivity) {
        mActivity = activity
        val a = activity.theme.obtainStyledAttributes(intArrayOf(
                android.R.attr.windowBackground
        ))
        val background = a.getResourceId(0, 0)
        a.recycle()
        val decor = activity.window.decorView as ViewGroup
        val decorChild = decor.getChildAt(0) as ViewGroup
        decorChild.setBackgroundResource(background)
        decor.removeView(decorChild)
        addView(decorChild)
        setContentView(decorChild)
        decor.addView(this)
    }

    fun attachToFragment(fragment: ISupportFragment?, view: View?) {
        addView(view)
        setFragment(fragment, view)
    }

    private fun setContentView(view: View) {
        mContentView = view
    }

    fun setEnableGesture(enable: Boolean) {
        mEnable = enable
    }

    fun setEdgeLevel(edgeLevel: EdgeLevel?) {
        validateEdgeLevel(-1, edgeLevel)
    }

    fun setEdgeLevel(widthPixel: Int) {
        validateEdgeLevel(widthPixel, null)
    }

    private fun validateEdgeLevel(widthPixel: Int, edgeLevel: EdgeLevel?) {
        try {
            val metrics = DisplayMetrics()
            val windowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(metrics)
            val mEdgeSize = viewDragHelper.javaClass.getDeclaredField("mEdgeSize")
            mEdgeSize.isAccessible = true
            if (widthPixel >= 0) {
                mEdgeSize.setInt(viewDragHelper, widthPixel)
            } else {
                when (edgeLevel) {
                    EdgeLevel.MAX -> {
                        mEdgeSize.setInt(viewDragHelper, metrics.widthPixels)
                    }
                    EdgeLevel.MED -> {
                        mEdgeSize.setInt(viewDragHelper, metrics.widthPixels / 2)
                    }
                    else -> {
                        mEdgeSize.setInt(viewDragHelper, (20 * metrics.density + 0.5f).toInt())
                    }
                }
            }
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    private inner class ViewDragCallback : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (isVerticalScroll) {
                Timber.d("tryCaptureView: false, isVerticalScroll=$isVerticalScroll")
                return false
            }

            val dragEnable = viewDragHelper.isEdgeTouched(mEdgeFlag, pointerId)
            if (dragEnable) {
                if (viewDragHelper.isEdgeTouched(EDGE_LEFT, pointerId)) {
                    mCurrentSwipeOrientation = EDGE_LEFT
                } else if (viewDragHelper.isEdgeTouched(EDGE_RIGHT, pointerId)) {
                    mCurrentSwipeOrientation = EDGE_RIGHT
                }
                mListeners?.forEach {
                    it.onEdgeTouch(mCurrentSwipeOrientation)
                }
                if (mPreFragment == null) {
                    (mFragment as? Fragment)?.let { f ->
                        FragmentationMagician.getActiveFragments(f.fragmentManager)?.let { fragmentList ->
                            if (fragmentList.isNotEmpty()) {
                                val index = fragmentList.indexOf(f)
                                kotlin.run {
                                    (index - 1 downTo 0).forEach { i ->
                                        val fragment = fragmentList[i]
                                        fragment.view?.let {
                                            it.visibility = View.VISIBLE
                                            mPreFragment = fragment
                                            return@run
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    mPreFragment?.view?.let {
                        if (it.visibility != View.VISIBLE) {
                            it.visibility = View.VISIBLE
                        }
                    }
                }
            }
            return dragEnable
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            var ret = 0
            if (mCurrentSwipeOrientation and EDGE_LEFT != 0) {
                ret = left.coerceAtLeast(0).coerceAtMost(child.width)
            } else if (mCurrentSwipeOrientation and EDGE_RIGHT != 0) {
                ret = left.coerceAtLeast(-child.width).coerceAtMost(0)
            }
            return ret
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            val contentView = mContentView ?: return

            Timber.d("onViewPositionChanged: left=$left, top=$top, dx=$dx, dy=$dy")

            if (mCurrentSwipeOrientation and EDGE_LEFT != 0) {
                mScrollPercent = abs(left.toFloat() / (contentView.width + (mShadowLeft?.intrinsicWidth
                        ?: 0)))
            } else if (mCurrentSwipeOrientation and EDGE_RIGHT != 0) {
                mScrollPercent = abs(left.toFloat() / (contentView.width + (mShadowRight?.intrinsicWidth
                        ?: 0)))
            }
            mContentLeft = left
            mContentTop = top
            invalidate()
            if (mListeners != null && viewDragHelper.viewDragState == STATE_DRAGGING && mScrollPercent <= 1 && mScrollPercent > 0) {
                mListeners?.forEach { it.onDragScrolled(mScrollPercent) }
            }

            if (mScrollPercent > 1) {
                if (mFragment != null) {
                    if (mCallOnDestroyView) return
                    if (!(mFragment as Fragment).isDetached) {
                        onDragFinished()
                        mFragment?.supportDelegate?.popQuiet()
                    }
                } else {
                    mActivity?.let {
                        if (!it.isFinishing) {
                            onDragFinished()
                            it.onBackPressed()
                            it.overridePendingTransition(0, 0)
                        }
                    }
                }
            }
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            if (mFragment != null) {
                return 1
            }
            return if (mActivity is ISwipeBackActivity && (mActivity as ISwipeBackActivity).swipeBackPriority()) {
                1
            } else 0
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val childWidth = releasedChild.width
            var left = 0
            val top = 0
            if (mCurrentSwipeOrientation and EDGE_LEFT != 0) {
                left = if ((abs(xvel) > abs(yvel) && xvel > 0)
                        || (xvel == 0f && mScrollPercent > mScrollFinishThreshold)) {
                    childWidth + (mShadowLeft?.intrinsicWidth ?: 0) + OVERSCROLL_DISTANCE
                } else {
                    0
                }
            } else if (mCurrentSwipeOrientation and EDGE_RIGHT != 0) {
                left = if (xvel < 0 || xvel == 0f && mScrollPercent > mScrollFinishThreshold) {
                    -(childWidth + (mShadowRight?.intrinsicWidth ?: 0) + OVERSCROLL_DISTANCE)
                } else {
                    0
                }
            }
            Timber.d("left=$left, top=$top, xvel=$xvel, yvel=$yvel")
            viewDragHelper.settleCapturedViewAt(left, top)
            invalidate()
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            mListeners?.forEach { listener ->
                listener.onDragStateChange(state)
            }
        }

        override fun onEdgeTouched(edgeFlags: Int, pointerId: Int) {
            super.onEdgeTouched(edgeFlags, pointerId)
            if (mEdgeFlag and edgeFlags != 0) {
                mCurrentSwipeOrientation = edgeFlags
            }
        }
    }

    private fun onDragFinished() {
        mListeners?.forEach { listener ->
            listener.onDragStateChange(STATE_FINISHED)
        }
    }

    private var downX = Float.NaN
    private var downY = Float.NaN
    private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var isVerticalScroll = false

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_UP) {
            isVerticalScroll = false
            Timber.d("dispatchTouchEvent: ACTION_UP")
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!mEnable) {
            return super.onInterceptTouchEvent(ev)
        }
        if (isVerticalScroll) {
            return super.onInterceptTouchEvent(ev)
        }
        try {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    isVerticalScroll = false
                    downX = ev.x
                    downY = ev.y
                }
                MotionEvent.ACTION_UP -> {
                    isVerticalScroll = false
                    downX = Float.NaN
                    downY = Float.NaN
                    Timber.d("onInterceptTouchEvent: ACTION_UP")
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isVerticalScroll) {
                        if (downX.isNaN() || downY.isNaN()) {
                            downY = ev.x
                            downY = ev.y
                        }
                        if (!downX.isNaN() && !downY.isNaN()) {
                            val dx = (ev.x - downX).absoluteValue
                            val dy = (ev.y - downY).absoluteValue
                            Timber.d("onInterceptTouchEvent: dx=$dx, dy=$dy, touchSlop=$touchSlop")
                            if (dx > touchSlop || dy > touchSlop) {
                                isVerticalScroll = dy >= dx
                                Timber.d("onInterceptTouchEvent: scroll happen isVerticalScroll=$isVerticalScroll")
                            }
                        }
                    }
                }
            }
            return viewDragHelper.shouldInterceptTouchEvent(ev)
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mEnable) {
            return super.onTouchEvent(event)
        }
        try {
            viewDragHelper.processTouchEvent(event)
            return true
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
        return false
    }

}