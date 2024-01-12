package com.example.accessibilityservice

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Path
import android.graphics.PixelFormat
import android.util.Log
import android.view.DragEvent
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import java.util.ArrayDeque
import java.util.Deque


class GlobalActionBarService : AccessibilityService() {
    var mLayout: FrameLayout? = null
    private var gestureDetector: GestureDetector? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //
    }

    override fun onInterrupt() {
        //
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onServiceConnected() {
        // Create an overlay and display the action bar
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        mLayout = FrameLayout(this)
        val lp = WindowManager.LayoutParams()
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        lp.format = PixelFormat.TRANSLUCENT
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        lp.gravity = Gravity.TOP
        val inflater = LayoutInflater.from(this)
        gestureDetector = GestureDetector(this, SingleTapConfirm())
        inflater.inflate(R.layout.action_bar, mLayout)
        wm.addView(mLayout, lp)
        val scrollButton = mLayout!!.findViewById<View>(R.id.scroll) as Button
        configureScrollButton()
        configurePowerButton()
        configureSwipeButton()

        scrollButton.setOnTouchListener(OnTouchListener { v, event ->
            if (gestureDetector!!.onTouchEvent(event)) {
                if (rootInActiveWindow != null) {
                    val y = Resources.getSystem().displayMetrics.heightPixels.toFloat()
                    val x = Resources.getSystem().displayMetrics.widthPixels.toFloat()
                    val swipePath = Path()
                    swipePath.moveTo(1000f, y/2)
                    swipePath.lineTo(1000f, 100f)
                    val gestureBuilder = GestureDescription.Builder()
                    gestureBuilder.addStroke(StrokeDescription(swipePath, 0, 100))
                    dispatchGesture(gestureBuilder.build(), null, null)
                }
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                /* You can play around with the offset to set where you want the users finger to be on the view. Currently it should be centered.*/
                val xOffset = v.width / 2
                val yOffset = v.height / 2
                val x = event.rawX.toInt() - xOffset
                val y = event.rawY.toInt() - yOffset
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    x, y,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
                params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                params.gravity = Gravity.TOP or Gravity.START
                wm.updateViewLayout(mLayout, params)
                return@OnTouchListener true
            }
            false
        })

//        mLayout?.setOnDragListener { v, event ->
//            when (event.action) {
//                DragEvent.ACTION_DRAG_STARTED->{
//                    Log.d("DRAG","Started")
//                    true
//                }
//                DragEvent.ACTION_DRAG_ENTERED -> {
//                    Log.d("DRAG","Entered")
//                    val xOffset = v.width / 2
//                    val yOffset = v.height / 2
//                    val x = event.x.toInt() - xOffset
//                    val y = event.y.toInt() - yOffset
//                    val params = WindowManager.LayoutParams(
//                        WindowManager.LayoutParams.WRAP_CONTENT,
//                        WindowManager.LayoutParams.WRAP_CONTENT,
//                        x, y,
//                        WindowManager.LayoutParams.TYPE_PHONE,
//                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
//                                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
//                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                        PixelFormat.TRANSLUCENT
//                    )
//                    params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
//                    params.gravity = Gravity.TOP or Gravity.START
//                    wm.updateViewLayout(mLayout, params)
//                    return@setOnDragListener true
//                }
//
//                else -> {
//                    false
//                }
//            }
//        }
    }


    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val deque: Deque<AccessibilityNodeInfo> = ArrayDeque<AccessibilityNodeInfo>()
        deque.add(root)
        while (!deque.isEmpty()) {
            val node: AccessibilityNodeInfo = deque.removeFirst()
            if (node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)) {
                return node
            }
            if (node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD))
                return node
            for (i in 0 until node.childCount) {
                deque.addLast(node.getChild(i))
            }
        }
        return null
    }

    private fun configureScrollButton() {
        val scrollButton = mLayout!!.findViewById<View>(R.id.scroll) as Button
        scrollButton.setOnClickListener {
            val scrollable = findScrollableNode(rootInActiveWindow)
            scrollable?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id)
        }
    }

    private fun configurePowerButton() {
        val scrollButton = mLayout!!.findViewById<View>(R.id.power) as Button
        scrollButton.setOnClickListener {
            val scrollable = findScrollableNode(rootInActiveWindow)
            scrollable?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.id)
        }
    }

    private fun configureSwipeButton() {
        val swipeButton = mLayout!!.findViewById<View>(R.id.swipe) as Button
        swipeButton.setOnClickListener { view ->
            val y = Resources.getSystem().displayMetrics.heightPixels.toFloat()
            val x = Resources.getSystem().displayMetrics.widthPixels.toFloat()
            val swipePath = Path()
//            swipePath.moveTo(1000f, 1000f)
//            swipePath.lineTo(100f, 1000f)
            swipePath.moveTo(x / 2, y)
            swipePath.lineTo(100f, 0f)
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(StrokeDescription(swipePath, 0, 100))
            dispatchGesture(gestureBuilder.build(), null, null)
        }
    }

    private class SingleTapConfirm : SimpleOnGestureListener() {
        override fun onSingleTapUp(event: MotionEvent): Boolean {
            return true
        }
    }
}