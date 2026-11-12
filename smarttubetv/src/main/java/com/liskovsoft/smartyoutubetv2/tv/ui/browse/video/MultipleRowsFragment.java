package com.liskovsoft.smartyoutubetv2.tv.ui.browse.video;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.RowPresenter.ViewHolder;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.interfaces.VideoGroupPresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.adapter.VideoGroupObjectAdapter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.ChannelHeaderPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.ChannelHeaderPresenter.ChannelHeaderCallback;
import com.liskovsoft.smartyoutubetv2.tv.presenter.ShortsCardPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.VideoCardPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.CustomListRowPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.OnItemLongPressedListener;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.interfaces.VideoSection;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.UriBackgroundManager;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MultipleRowsFragment extends RowsSupportFragment implements VideoSection {
    private static final String TAG = MultipleRowsFragment.class.getSimpleName();
    private UriBackgroundManager mBackgroundManager;
    private ArrayObjectAdapter mRowsAdapter;
    private CustomListRowPresenter mRowPresenter;
    private Map<Integer, VideoGroupObjectAdapter> mVideoGroupAdapters;
    private final List<VideoGroup> mPendingUpdates = new ArrayList<>();
    private VideoGroupPresenter mMainPresenter;
    private VideoCardPresenter mCardPresenter;
    private ShortsCardPresenter mShortsPresenter;
    private int mSelectedRowIndex = -1;
    private ChannelHeaderCallback mChannelHeaderCallback;
    private VideoCardPresenter mHeroCardPresenter;
    private ListRow mHeroRow;
    private View mHero;
    private boolean mHeroShown;
    private int mDefaultAlignTop;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mMainPresenter = getMainPresenter();
        mCardPresenter = new VideoCardPresenter();
        mShortsPresenter = new ShortsCardPresenter();
        mHeroCardPresenter = new HeroCardPresenter();
        mBackgroundManager = ((LeanbackActivity) getActivity()).getBackgroundManager();

        setupAdapter();
        setupEventListeners();
        applyPendingUpdates();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rows = super.onCreateView(inflater, container, savedInstanceState);

        if (!ViewUtil.isYouTubeStyle(getContext())) {
            return rows;
        }

        // The featured video is drawn behind the rows
        FrameLayout root = new FrameLayout(inflater.getContext());
        // Null when declared via a <fragment> tag (ChannelFragment)
        if (rows.getLayoutParams() != null) {
            root.setLayoutParams(rows.getLayoutParams());
        }
        mHero = inflater.inflate(R.layout.youtube_hero, root, false);
        mHeroShown = false;
        root.addView(mHero);
        root.addView(rows, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    @Override
    public void onDestroyView() {
        updateHero(null);
        mHero = null;
        super.onDestroyView();
    }

    @Override
    public void setAlignment(int windowAlignOffsetFromTop) {
        super.setAlignment(windowAlignOffsetFromTop);

        if (mHeroShown && getVerticalGridView() != null) { // keep the featured row low
            mDefaultAlignTop = getVerticalGridView().getWindowAlignmentOffset();
            getVerticalGridView().setWindowAlignmentOffset(getHeroAlignTop());
        }
    }

    /**
     * YouTube color scheme: the first row is featured, the selected card is shown big above it
     */
    protected boolean isHeroEnabled() {
        return false;
    }

    protected void addHeader(ChannelHeaderCallback callback) {
        mChannelHeaderCallback = callback;
    }

    protected abstract VideoGroupPresenter getMainPresenter();

    private void applyPendingUpdates() {
        // prevent modification within update method
        List<VideoGroup> copyArray = new ArrayList<>(mPendingUpdates);

        mPendingUpdates.clear();

        for (VideoGroup group : copyArray) {
            update(group);
        }
    }

    private void setupAdapter() {
        if (mVideoGroupAdapters == null) {
            mVideoGroupAdapters = new HashMap<>();
        }

        if (mRowsAdapter == null) {
            mRowPresenter = new CustomListRowPresenter(ViewUtil.getFocusZoomFactor(getContext()));
            mRowPresenter.enableChildRoundedCorners(getMainUIData().isUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS));
            mRowPresenter.setShadowEnabled(!ViewUtil.isYouTubeStyle(getContext()));

            ClassPresenterSelector presenterSelector = new ClassPresenterSelector();

            if (ViewUtil.isYouTubeStyle(getContext())) {
                // The featured row keeps its small cards, it isn't enlarged when selected
                CustomListRowPresenter heroRowPresenter = new CustomListRowPresenter(ViewUtil.getFocusZoomFactor(getContext()));
                heroRowPresenter.setShadowEnabled(false);
                presenterSelector.addClassPresenter(HeroRow.class, heroRowPresenter);
                mRowPresenter.setSelectedRowCardScale(ViewUtil.YOUTUBE_SELECTED_ROW_CARD_SCALE);
                int top = getResources().getDimensionPixelSize(R.dimen.youtube_row_padding_top);
                int bottom = getResources().getDimensionPixelSize(R.dimen.youtube_row_padding_bottom);
                mRowPresenter.setExpandedRowPadding(top, bottom);
                heroRowPresenter.setExpandedRowPadding(top, bottom);
            }

            presenterSelector.addClassPresenter(ListRow.class, mRowPresenter);
            presenterSelector.addClassPresenter(ChannelHeaderCallback.class, new ChannelHeaderPresenter());

            mRowsAdapter = new ArrayObjectAdapter(presenterSelector);
            setAdapter(mRowsAdapter);
        }
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
        mCardPresenter.setOnItemViewLongPressedListener(new ItemViewLongPressedListener());
        mShortsPresenter.setOnItemViewLongPressedListener(new ItemViewLongPressedListener());
        mHeroCardPresenter.setOnItemViewLongPressedListener(new ItemViewLongPressedListener());
    }

    @Override
    public void clear() {
        mHeroRow = null;
        updateHero(null);

        if (mRowsAdapter != null) {
            mRowsAdapter.clear();
            if (mChannelHeaderCallback != null) {
                mRowsAdapter.add(mChannelHeaderCallback);
            }
        }

        if (mVideoGroupAdapters != null) {
            mVideoGroupAdapters.clear();
        }

        // Reset the position (bug appeared after fragment been reused)
        setPosition(mChannelHeaderCallback != null ? 1 : 0);
    }

    private void removeByIndex(int idx) {
        if (mRowsAdapter != null && mRowsAdapter.size() > idx) {
            ListRow row = (ListRow) mRowsAdapter.get(idx);
            mRowsAdapter.remove(row);
            VideoGroupObjectAdapter group = (VideoGroupObjectAdapter) row.getAdapter();
            mVideoGroupAdapters.values().remove(group);
        }
    }

    private void removeById(int id) {
        if (mRowsAdapter != null) {
            VideoGroupObjectAdapter needed = mVideoGroupAdapters.get(id);
            for (int i = 0; i < mRowsAdapter.size(); i++) {
                Object row = mRowsAdapter.get(i);

                if (row instanceof ListRow) {
                    VideoGroupObjectAdapter adapter = (VideoGroupObjectAdapter) ((ListRow) row).getAdapter();
                    if (adapter == needed) {
                        mRowsAdapter.remove(row);
                        mVideoGroupAdapters.remove(id);
                    }
                }
            }
        }
    }

    private int findPositionById(int id) {
        if (mRowsAdapter != null) {
            VideoGroupObjectAdapter needed = mVideoGroupAdapters.get(id);
            for (int i = 0; i < mRowsAdapter.size(); i++) {
                Object row = mRowsAdapter.get(i);

                if (row instanceof ListRow) {
                    VideoGroupObjectAdapter adapter = (VideoGroupObjectAdapter) ((ListRow) row).getAdapter();
                    if (adapter == needed) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    private boolean isComputingLayout(VideoGroup group) {
        int action = group.getAction();

        // Attempt to fix: IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
        if ((action == VideoGroup.ACTION_SYNC || action == VideoGroup.ACTION_REPLACE) && getVerticalGridView() != null) {
            if (getVerticalGridView().isComputingLayout()) {
                return true;
            }
            int position = findPositionById(group.getId());
            if (position != -1) {
                RecyclerView.ViewHolder viewHolder = getVerticalGridView().findViewHolderForAdapterPosition(position);
                if (viewHolder != null) {
                    Object nestedRecyclerView = Helpers.getField(viewHolder, "mNestedRecyclerView");
                    if (nestedRecyclerView instanceof WeakReference) {
                        Object recyclerView = ((WeakReference<?>) nestedRecyclerView).get();
                        return recyclerView instanceof RecyclerView && ((RecyclerView) recyclerView).isComputingLayout();
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean isEmpty() {
        if (mRowsAdapter == null) {
            return mPendingUpdates.isEmpty();
        }

        return mRowsAdapter.size() == 0;
    }

    @Override
    public void update(VideoGroup group) {
        if (isComputingLayout(group)) {
            return;
        }

        if (mVideoGroupAdapters == null) {
            mPendingUpdates.add(group);
            return;
        }

        // Correct position depending on the search bar presence
        if (group.getPosition() != -1 && mChannelHeaderCallback != null) {
            group.setPosition(group.getPosition() + 1);
        }

        int action = group.getAction();

        if (action == VideoGroup.ACTION_REPLACE) {
            if (group.getPosition() == -1) {
                clear();
            } else {
                removeById(group.getId());
            }
        } else if (action == VideoGroup.ACTION_REMOVE) {
            VideoGroupObjectAdapter adapter = mVideoGroupAdapters.get(group.getId());
            if (adapter != null) {
                adapter.remove(group);
            }
            return;
        } else if (action == VideoGroup.ACTION_SYNC) {
            VideoGroupObjectAdapter adapter = mVideoGroupAdapters.get(group.getId());
            if (adapter != null) {
                freeze(true);
                adapter.sync(group);
                freeze(false);
            }
            return;
        }

        if (group.isEmpty()) {
            return;
        }

        VideoGroupObjectAdapter existingAdapter = GridFragmentHelper.findRelatedAdapter(mVideoGroupAdapters, group, this::freeze);

        if (existingAdapter == null) {
            boolean isHeroRow = mHero != null && mRowsAdapter.size() == 0 && !group.isShorts() && isHeroEnabled();
            // The featured row has no title, like YouTube
            HeaderItem rowHeader = new HeaderItem(isHeroRow ? "" : group.getTitle());
            int videoGroupId = group.getId(); // Create unique int from category.

            VideoGroupObjectAdapter videoGroupAdapter = new VideoGroupObjectAdapter(group,
                    group.isShorts() ? mShortsPresenter : isHeroRow ? mHeroCardPresenter : mCardPresenter);

            mVideoGroupAdapters.put(videoGroupId, videoGroupAdapter);

            ListRow row = isHeroRow ? new HeroRow(rowHeader, videoGroupAdapter) : new ListRow(rowHeader, videoGroupAdapter);

            if (isHeroRow) {
                mHeroRow = row;
            }

            if (group.getPosition() == -1 || group.getPosition() > mRowsAdapter.size()) {
                mRowsAdapter.add(row);
            } else {
                mRowsAdapter.add(group.getPosition(), row);
            }
        } else {
            Log.d(TAG, "Continue row %s %s", group.getTitle(), System.currentTimeMillis());

            freeze(true);

            existingAdapter.add(group); // continue

            freeze(false);
        }

        restorePosition();
    }

    private void restorePosition() {
        setPosition(mSelectedRowIndex);

        // Maybe we don't need to load next group since all rows already fetched?
    }

    @Override
    public int getPosition() {
        return getSelectedPosition();
    }

    @Override
    public void setPosition(int index) {
        if (index < 0) {
            return;
        }

        if (mRowsAdapter != null && index < mRowsAdapter.size()) {
            setSelectedPosition(index, false);
            mSelectedRowIndex = -1;
        } else {
            mSelectedRowIndex = index;
        }
    }

    @Override
    public void selectItem(Video item) {
        // NOP
    }

    /**
     * Disable scrolling on partially updated rows. This prevent cards from misbehaving.
     */
    private void freeze(boolean freeze) {
        // Disable scrolling on partially updated rows. This prevent controls from misbehaving.
        if (mRowPresenter != null) {
            ViewHolder vh = getRowViewHolder(getSelectedPosition());
            if (vh instanceof ListRowPresenter.ViewHolder) {
                mRowPresenter.freeze(vh, freeze);
            }
        }
    }

    private void updateHero(Video video) {
        if (mHero == null || getVerticalGridView() == null) {
            return;
        }

        if (video != null) {
            ((TextView) mHero.findViewById(R.id.hero_title)).setText(video.getTitle());
            ((TextView) mHero.findViewById(R.id.hero_subtitle)).setText(video.getSecondTitle());
            Glide.with(mHero.getContext())
                    .load(video.bgImageUrl != null ? video.bgImageUrl : video.cardImageUrl)
                    .apply(ViewUtil.glideOptions())
                    .into((ImageView) mHero.findViewById(R.id.hero_image));
        }

        boolean show = video != null;

        if (show == mHeroShown) {
            return;
        }

        mHeroShown = show;
        mHero.setVisibility(show ? View.VISIBLE : View.GONE);

        VerticalGridView grid = getVerticalGridView();
        if (show) {
            mDefaultAlignTop = grid.getWindowAlignmentOffset();
        }
        grid.setWindowAlignmentOffset(show ? getHeroAlignTop() : mDefaultAlignTop);

        // The picture of the featured video continues under the title bar
        Fragment parent = getParentFragment();
        View titleView = parent instanceof BrowseSupportFragment ? ((BrowseSupportFragment) parent).getTitleView() : null;
        if (titleView != null) {
            titleView.setBackgroundColor(show ? Color.TRANSPARENT : ContextCompat.getColor(mHero.getContext(), R.color.scheme_youtube_background));
        }
    }

    private int getHeroAlignTop() {
        return mDefaultAlignTop + getResources().getDimensionPixelSize(R.dimen.youtube_hero_row_shift);
    }

    private static class HeroRow extends ListRow {
        HeroRow(HeaderItem header, ObjectAdapter adapter) {
            super(header, adapter);
        }
    }

    /**
     * Small thumbnails without text, their details are shown in the featured area above the row
     */
    private static class HeroCardPresenter extends VideoCardPresenter {
        private static final float SCALE = 0.55f;

        @Override
        protected Pair<Integer, Integer> getCardDimensPx(Context context) {
            Pair<Integer, Integer> size = super.getCardDimensPx(context);
            return new Pair<>((int) (size.first * SCALE), (int) (size.second * SCALE));
        }

        @Override
        protected boolean isTitleEnabled() {
            return false;
        }

        @Override
        protected boolean isContentEnabled() {
            return false;
        }
    }

    private MainUIData getMainUIData() {
        return MainUIData.instance(getContext());
    }

    private final class ItemViewLongPressedListener implements OnItemLongPressedListener {
        @Override
        public void onItemLongPressed(Presenter.ViewHolder itemViewHolder, Object item) {

            if (item instanceof Video) {
                mMainPresenter.onVideoItemLongClicked((Video) item);
            } else {
                Toast.makeText(getActivity(), item.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final class ItemViewClickedListener implements androidx.leanback.widget.OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                mMainPresenter.onVideoItemClicked((Video) item);
            } else {
                Toast.makeText(getActivity(), item.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Video) {
                updateHero(row == mHeroRow ? (Video) item : null);

                mBackgroundManager.setBackgroundFrom((Video) item);

                mMainPresenter.onVideoItemSelected((Video) item);

                checkScrollEnd((Video)item);
            }
        }

        private void checkScrollEnd(Video item) {
            for (VideoGroupObjectAdapter adapter : mVideoGroupAdapters.values()) {
                int index = adapter.indexOf(item);

                if (index != -1) {
                    int size = adapter.size();
                    if (index > (size - ViewUtil.ROW_SCROLL_CONTINUE_NUM)) {
                        mMainPresenter.onScrollEnd((Video) adapter.get(size - 1));
                    }
                    break;
                }
            }
        }
    }
}
