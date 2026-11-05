package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.view.View;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.complexcardview.ComplexImageCardView;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class CustomListRowPresenter extends ListRowPresenter {
    private float mSelectedRowCardScale = 1;

    public CustomListRowPresenter() {
        this(ViewUtil.FOCUS_ZOOM_FACTOR);
    }

    public CustomListRowPresenter(int zoomFactor) {
        super(zoomFactor, ViewUtil.FOCUS_DIMMER_ENABLED);
        setSelectEffectEnabled(ViewUtil.ROW_SELECT_EFFECT_ENABLED);
    }

    /**
     * YouTube color scheme: cards of the selected row are bigger than the rest
     */
    public void setSelectedRowCardScale(float scale) {
        mSelectedRowCardScale = scale;
    }

    @Override
    protected void onRowViewSelected(RowPresenter.ViewHolder holder, boolean selected) {
        super.onRowViewSelected(holder, selected);

        if (mSelectedRowCardScale != 1) {
            HorizontalGridView gridView = ((ViewHolder) holder).getGridView();
            for (int i = 0; i < gridView.getChildCount(); i++) {
                applySelectLevelToChild((ViewHolder) holder, gridView.getChildAt(i));
            }
        }
    }

    @Override
    protected void applySelectLevelToChild(ViewHolder rowViewHolder, View childView) { // also called for every new card of the row
        super.applySelectLevelToChild(rowViewHolder, childView);

        if (mSelectedRowCardScale != 1 && childView instanceof ComplexImageCardView) {
            ((ComplexImageCardView) childView).setMainImageScale(rowViewHolder.isSelected() ? mSelectedRowCardScale : 1);
        }
    }
}
