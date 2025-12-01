package indi.etern.musichud.client.ui.components;

import icyllis.modernui.core.Context;
import icyllis.modernui.view.MeasureSpec;
import icyllis.modernui.widget.LinearLayout;

public class WrappingLinearLayout extends LinearLayout {
    private int mWrapWidth;
    private int mMaxWidth;

    public WrappingLinearLayout(Context context) {
        super(context);
    }

    public void setWrapWidth(int wrapWidth) {
        mWrapWidth = wrapWidth;
    }

    public int getWrapWidth() {
        return mWrapWidth;
    }

    public void setMaxWidth(int maxWidth) {
        mMaxWidth = maxWidth;
    }

    public int getMaxWidth() {
        return mMaxWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        var orientation = width >= mWrapWidth
                ? HORIZONTAL : VERTICAL;
        setOrientation(orientation);
        if (mMaxWidth > 0) {
            int limit = orientation == HORIZONTAL
                    ? getChildCount() * mMaxWidth : mMaxWidth;
            if (width > limit) {
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(limit, MeasureSpec.EXACTLY);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
