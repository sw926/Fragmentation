package me.yokeyword.fragmentation_swipeback.core

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import me.yokeyword.fragmentation.ISupportActivity
import me.yokeyword.fragmentation.SwipeBackLayout
import me.yokeyword.fragmentation.SwipeBackLayout.EdgeLevel

/**
 * Created by YoKey on 17/6/29.
 */
class SwipeBackActivityDelegate(swipeBackActivity: ISwipeBackActivity?) {
    private val mActivity: FragmentActivity
    var swipeBackLayout: SwipeBackLayout? = null
        private set

    fun onCreate(savedInstanceState: Bundle?) {
        onActivityCreate()
    }

    fun onPostCreate(savedInstanceState: Bundle?) {
        swipeBackLayout?.attachToActivity(mActivity)
    }

    fun setSwipeBackEnable(enable: Boolean) {
        swipeBackLayout?.setEnableGesture(enable)
    }

    fun setEdgeLevel(edgeLevel: EdgeLevel?) {
        swipeBackLayout?.setEdgeLevel(edgeLevel)
    }

    fun setEdgeLevel(widthPixel: Int) {
        swipeBackLayout?.setEdgeLevel(widthPixel)
    }

    /**
     * 限制SwipeBack的条件,默认栈内Fragment数 <= 1时 , 优先滑动退出Activity , 而不是Fragment
     *
     * @return true: Activity可以滑动退出, 并且总是优先;  false: Fragment优先滑动退出
     */
    fun swipeBackPriority(): Boolean {
        return mActivity.supportFragmentManager.backStackEntryCount <= 1
    }

    private fun onActivityCreate() {
        mActivity.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mActivity.window.decorView.setBackgroundColor(Color.TRANSPARENT)
        swipeBackLayout = SwipeBackLayout(mActivity)
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        swipeBackLayout?.layoutParams = params
    }

    init {
        if (swipeBackActivity !is FragmentActivity || swipeBackActivity !is ISupportActivity) throw RuntimeException("Must extends FragmentActivity/AppCompatActivity and implements ISupportActivity")
        mActivity = swipeBackActivity
    }
}