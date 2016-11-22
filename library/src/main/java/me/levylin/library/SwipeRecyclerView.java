package me.levylin.library;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import me.levylin.library.view.SwipeMenuLayout;

public class SwipeRecyclerView extends RecyclerView {
    private static final int TOUCH_STATE_NONE = 0;  //没有滑动
    private static final int TOUCH_STATE_X = 1;     //是否处于左右滑动
    private int mTouchState = TOUCH_STATE_NONE;     //当前状态

    private float mDownX;                           //按下去的X坐标
    private float mDownY;                           //按下去的Y坐标
    private Rect mTouchFrame = new Rect();          //点击后坐标产生的矩形Rect
    private int mTouchPosition = -1;                //当前按下去的位置
    private int oldPos = -1;                        //上一次按下去的位置
    private SwipeMenuLayout mTouchView;             //当前按下去的位置的那个view
    private int mTouchSlop;                         //是否达到了滑动的距离
    private OnSwipeListener mOnSwipeListener;

    public SwipeRecyclerView(Context context) {
        super(context);
        init();
    }

    public SwipeRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SwipeRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                //是否拦截的返回值
                boolean handled = super.onInterceptTouchEvent(event);
                mTouchState = TOUCH_STATE_NONE;
                oldPos = mTouchPosition;
                //找到当前点击坐标下的所处于SwapRecyclerView的位置
                int mFirstPosition = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
                int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);
                    if (child.getVisibility() == View.VISIBLE) {
                        child.getHitRect(mTouchFrame);
                        //判断是否点击到该控件上
                        if (mTouchFrame.contains(x, y)) {
                            mTouchPosition = mFirstPosition + i;
                            break;
                        }
                    }
                }
                //找到了
                if (mTouchPosition != -1) {
                    //通过position得到item的viewHolder,并判断合法性
                    View view = getChildAt(mTouchPosition - mFirstPosition);
                    if (view != null) {
                        RecyclerView.ViewHolder viewHolder = getChildViewHolder(view);
                        //menuView处于打开且点击的不在menu区域
                        if (mTouchView != null && mTouchView.isOpen() && !inRangeOfView(mTouchView.getmMenuView(), event)) {
                            //拦截事件,交给自己的onTouch方法处理.
                            return true;
                        }
                        if (viewHolder.itemView instanceof SwipeMenuLayout) {
                            mTouchView = (SwipeMenuLayout) view;
                        }

                    }
                    //将事件交给SwipeMenuLayout处理down事件
                    if (mTouchView != null) {
                        mTouchView.onSwipe(event);
                    }
                }
                //down事件,如果没有打开menu,则不拦截,仍然交给系统
                return handled;
            case MotionEvent.ACTION_MOVE:
                calculateTouchState(event);
                if (mTouchState == TOUCH_STATE_X) {
                    return true;//拦截事件,交给自己的onTouch方法处理.
                }
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN && mTouchView == null)
            return super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //如果当前是处于打开的且用户按下去正好是打开menu的那行
                if (mTouchPosition == oldPos && mTouchView != null && mTouchView.isOpen()) {
                    mTouchState = TOUCH_STATE_X;
                    mTouchView.onSwipe(event);
                    return true;
                } else {
                    //如果不是直接关闭
                    if (mTouchView != null && mTouchView.isOpen()) {
                        mTouchView.smoothCloseMenu();
                        mTouchView = null;
                        return super.onTouchEvent(event);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchState != TOUCH_STATE_X) {//如果不是左右滑动状态，则重新验证状态
                    //左右滑动交给mTouchView处理,事件消费了
                    calculateTouchState(event);
                }
                if (mTouchState == TOUCH_STATE_X) {
                    if (mTouchView != null) {
                        mTouchView.onSwipe(event);
                    }
                    event.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(event);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                //处于左右滑动
                if (mTouchState == TOUCH_STATE_X) {
                    if (mTouchView != null) {
                        mTouchView.onSwipe(event);//打开活着关闭
                        if (!mTouchView.isOpen()) {//关闭后复原变量
                            mTouchPosition = -1;
                            mTouchView = null;
                        } else {
                            mTouchView.getmMenuView().setPosition(mTouchPosition);
                        }
                    }
                    if (mOnSwipeListener != null) {
                        mOnSwipeListener.onSwipeEnd(mTouchPosition);
                    }
                    event.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(event);
                    return true;
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 计算点击状态
     *
     * @param event 点击事件
     */
    private void calculateTouchState(MotionEvent event) {
        float dx = Math.abs(event.getX() - mDownX);
        float dy = Math.abs(event.getY() - mDownY);
        //达到了滑动的临界值
        if (dx > mTouchSlop && dx > dy) {
            if (mTouchState == TOUCH_STATE_NONE) {
                mTouchState = TOUCH_STATE_X;
                getParent().requestDisallowInterceptTouchEvent(true);
                if (mOnSwipeListener != null) {
                    mOnSwipeListener.onSwipeStart(mTouchPosition);
                }
            }
        }
    }

    /**
     * 判断点击事件是否在某个view内
     *
     * @param view view
     * @param ev   ev
     * @return true 是 false 不是
     */
    public static boolean inRangeOfView(View view, MotionEvent ev) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        if (ev.getRawX() < x || ev.getRawX() > (x + view.getWidth()) || ev.getRawY() < y || ev.getRawY() > (y + view.getHeight())) {
            return false;
        }
        return true;
    }

    public void smoothCloseMenu(int position) {
        int mFirstPosition = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
        if (position >= 0) {
            View view = getChildAt(position - mFirstPosition);
            RecyclerView.ViewHolder viewHolder = getChildViewHolder(view);
            if (viewHolder.itemView instanceof SwipeMenuLayout) {
                SwipeMenuLayout swipeMenuLayout = (SwipeMenuLayout) viewHolder.itemView;
                if (swipeMenuLayout.isOpen()) {
                    swipeMenuLayout.smoothCloseMenu();
                }
            }
        }
    }

    public void setOnSwipeListener(OnSwipeListener onSwipeListener) {
        this.mOnSwipeListener = onSwipeListener;
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        if (mTouchView != null) {//删除视图之后，重置点击视图
            mTouchView.closeMenu();
            mTouchView = null;
        }
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (mTouchView != null) {//添加视图之后，重置点击视图
            mTouchView.closeMenu();
            mTouchView = null;
        }
    }

    @Override
    public boolean canScrollVertically(int direction) {//当处于左右滑动状态时，返回true，让外部的下拉控件误以为列表可下滑，从而拦截下拉刷新事件
        return mTouchState == TOUCH_STATE_X || super.canScrollVertically(direction);
    }

    public interface OnSwipeListener {
        void onSwipeStart(int position);

        void onSwipeEnd(int position);
    }
}
