package me.levylin.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import me.levylin.library.view.SwipeMenuLayout;
import me.levylin.library.view.SwipeMenuView;

public class SwipeWrapperUtils {

    /***
     * 包装RecyclerView的item
     *
     * @param parent            parent
     * @param layoutId          layoutId
     * @param menuView          menuView 菜单的view
     * @param closeInterpolator closeInterpolator 关闭的差值器
     * @param openInterpolator  openInterpolator  打开的差值器
     * @return SwipeMenuLayout 包装完后的SwipeMenuLayout
     */
    public static SwipeMenuLayout wrap(ViewGroup parent, int layoutId, SwipeMenuView menuView, Interpolator closeInterpolator, Interpolator openInterpolator) {
        View contentView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new SwipeMenuLayout(contentView, menuView, closeInterpolator, openInterpolator);
    }
}
