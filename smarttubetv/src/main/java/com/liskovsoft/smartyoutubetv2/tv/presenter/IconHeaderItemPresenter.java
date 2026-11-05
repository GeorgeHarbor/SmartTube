package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowHeaderPresenter;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class IconHeaderItemPresenter extends RowHeaderPresenter {
    private static final String TAG = IconHeaderItemPresenter.class.getSimpleName();
    // YouTube color scheme: #B7B7B7 icons next to the #F1F1F1 current section in the closed sidebar
    private static final float YOUTUBE_UNSELECTED_ALPHA = 0.76f;
    /**
     * YouTube color scheme: Settings, when it's the last section, sits at the bottom of the sidebar
     */
    public static final RecyclerView.ItemDecoration YOUTUBE_SETTINGS_AT_BOTTOM = new RecyclerView.ItemDecoration() {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
            int count = state.getItemCount();
            if (!(holder instanceof ItemBridgeAdapter.ViewHolder) || holder.getAdapterPosition() != count - 1
                    || !(((ItemBridgeAdapter.ViewHolder) holder).getPresenter() instanceof IconHeaderItemPresenter)
                    || ((IconHeaderItemPresenter) ((ItemBridgeAdapter.ViewHolder) holder).getPresenter()).mResId != R.drawable.icon_settings) {
                return;
            }
            float density = parent.getResources().getDisplayMetrics().density;
            int height = parent.getHeight() > 0 ? parent.getHeight() : parent.getResources().getDisplayMetrics().heightPixels;
            // Rows are 39dp (icon_header_item_youtube minHeight), the last one ends 28.5dp above the screen edge
            int free = height - parent.getPaddingTop() - Math.round(count * 39 * density) - Math.round(28.5f * density);
            outRect.top = Math.max(0, free);
        }
    };
    private static boolean sYouTubeSidebarExpanded; // shortcut: one browse sidebar at a time, fine while there is only BrowseFragment
    private float mUnselectedAlpha;
    private final int mResId;
    private final String mIconUrl;
    private Drawable mDefaultIcon;
    private int[] mYouTubeIcons;

    public IconHeaderItemPresenter(int resId, String iconUrl) {
        mResId = resId;
        mIconUrl = iconUrl;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        mUnselectedAlpha = viewGroup.getResources()
                .getFraction(R.fraction.lb_browse_header_unselect_alpha, 1, 1);
        LayoutInflater inflater = (LayoutInflater) viewGroup.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDefaultIcon = new ColorDrawable(ContextCompat.getColor(viewGroup.getContext(), R.color.lb_grey));

        View view = inflater.inflate(ViewUtil.isYouTubeStyle(viewGroup.getContext()) ? R.layout.icon_header_item_youtube : R.layout.icon_header_item, null);
        view.setAlpha(ViewUtil.isYouTubeStyle(viewGroup.getContext()) ? (sYouTubeSidebarExpanded ? 1 : YOUTUBE_UNSELECTED_ALPHA) : mUnselectedAlpha); // Initialize icons to be at half-opacity.

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        HeaderItem headerItem;

        if (item instanceof PageRow) {
            headerItem = ((PageRow) item).getHeaderItem();
        } else {
            headerItem = ((ListRow) item).getHeaderItem();
        }

        View rootView = viewHolder.view;
        rootView.setFocusable(true);

        ImageView iconView = rootView.findViewById(R.id.header_icon);
        if (iconView != null) {
            if (mIconUrl != null) {
                Glide.with(rootView.getContext())
                        .load(mIconUrl)
                        .apply(ViewUtil.glideOptions().error(mDefaultIcon))
                        .listener(mErrorListener)
                        .into(iconView);

                //ViewUtil.makeMonochrome(iconView);
            } else {
                mYouTubeIcons = ViewUtil.isYouTubeStyle(rootView.getContext()) ? getYouTubeIcons(mResId) : null;
                if (mYouTubeIcons != null) {
                    // Glyphs keep their own sizes, like in the official app
                    iconView.setScaleType(ImageView.ScaleType.CENTER);
                    iconView.setImageResource(mYouTubeIcons[0]);
                } else {
                    Drawable icon = mResId > 0 ? ContextCompat.getDrawable(rootView.getContext(), mResId) : mDefaultIcon;
                    iconView.setImageDrawable(icon);
                }

                // Dark icon on the white pill of the focused item. Channel avatars (loaded by url) stay untinted.
                if (VERSION.SDK_INT >= 21 && ViewUtil.isYouTubeStyle(rootView.getContext())) {
                    iconView.setImageTintList(ContextCompat.getColorStateList(rootView.getContext(), R.color.youtube_sidebar_item_text));
                }
            }
        }

        TextView label = rootView.findViewById(R.id.header_label);
        if (label != null) {
            label.setText(headerItem.getName());
        }
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        // Bind resets the icon, the selected one is known only inside the list
        for (Object parent = holder.view.getParent(); parent instanceof View; parent = ((View) parent).getParent()) {
            if (parent instanceof VerticalGridView) {
                updateYouTubeIcons((VerticalGridView) parent);
                break;
            }
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // NOP
    }

    // TODO: This is a temporary fix. Remove me when leanback onCreateViewHolder no longer sets the
    // mUnselectAlpha, and also assumes the xml inflation will return a RowHeaderView.
    @Override
    protected void onSelectLevelChanged(RowHeaderPresenter.ViewHolder holder) {
        if (ViewUtil.isYouTubeStyle(holder.view.getContext())) {
            holder.view.setAlpha(sYouTubeSidebarExpanded || holder.getSelectLevel() > 0 ? 1 : YOUTUBE_UNSELECTED_ALPHA);
            return;
        }

        holder.view.setAlpha(mUnselectedAlpha + holder.getSelectLevel() *
                (1.0f - mUnselectedAlpha));
    }

    /**
     * YouTube color scheme: outline icon and the filled one of the current section
     */
    private static int[] getYouTubeIcons(int resId) {
        if (resId == R.drawable.icon_home) {
            return new int[] {R.drawable.youtube_ic_home, R.drawable.youtube_ic_home_filled};
        } else if (resId == R.drawable.icon_shorts) {
            return new int[] {R.drawable.youtube_ic_shorts, R.drawable.youtube_ic_shorts_filled};
        } else if (resId == R.drawable.icon_subscriptions) {
            return new int[] {R.drawable.youtube_ic_subscriptions, R.drawable.youtube_ic_subscriptions_filled};
        } else if (resId == R.drawable.icon_playlist) {
            return new int[] {R.drawable.youtube_ic_library, R.drawable.youtube_ic_library_filled};
        } else if (resId == R.drawable.icon_music) {
            return new int[] {R.drawable.youtube_ic_music, R.drawable.youtube_ic_music_filled};
        } else if (resId == R.drawable.icon_settings) {
            return new int[] {R.drawable.youtube_ic_settings, R.drawable.youtube_ic_settings_filled};
        } else if (resId == R.drawable.icon_gaming) {
            return new int[] {R.drawable.youtube_ic_gaming, R.drawable.youtube_ic_gaming};
        } else if (resId == R.drawable.icon_news) {
            return new int[] {R.drawable.youtube_ic_news, R.drawable.youtube_ic_news};
        } else if (resId == R.drawable.icon_sports) {
            return new int[] {R.drawable.youtube_ic_sports, R.drawable.youtube_ic_sports};
        } else if (resId == R.drawable.icon_history) {
            return new int[] {R.drawable.youtube_ic_history, R.drawable.youtube_ic_history};
        }
        return null;
    }

    /**
     * YouTube color scheme: every item is bright while the sidebar is open
     */
    public static void applyYouTubeSidebarState(VerticalGridView headers, boolean expanded) {
        sYouTubeSidebarExpanded = expanded;
        for (int i = 0; i < headers.getChildCount(); i++) {
            View child = headers.getChildAt(i);
            RecyclerView.ViewHolder holder = headers.getChildViewHolder(child);
            if (!(holder instanceof ItemBridgeAdapter.ViewHolder)) {
                continue;
            }
            // Grid children are wrappers, the alpha belongs to the header view inside
            View item = ((ItemBridgeAdapter.ViewHolder) holder).getViewHolder().view;
            boolean selected = holder.getAdapterPosition() == headers.getSelectedPosition();
            item.setAlpha(expanded || selected ? 1 : YOUTUBE_UNSELECTED_ALPHA);
        }
        updateYouTubeIcons(headers);
    }

    /**
     * YouTube color scheme: the current section has the filled icon, the rest are outlined
     */
    public static void updateYouTubeIcons(VerticalGridView headers) {
        for (int i = 0; i < headers.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = headers.getChildViewHolder(headers.getChildAt(i));
            if (!(holder instanceof ItemBridgeAdapter.ViewHolder)
                    || !(((ItemBridgeAdapter.ViewHolder) holder).getPresenter() instanceof IconHeaderItemPresenter)) {
                continue;
            }
            int[] icons = ((IconHeaderItemPresenter) ((ItemBridgeAdapter.ViewHolder) holder).getPresenter()).mYouTubeIcons;
            ImageView iconView = ((ItemBridgeAdapter.ViewHolder) holder).getViewHolder().view.findViewById(R.id.header_icon);
            if (icons != null && iconView != null) {
                iconView.setImageResource(icons[holder.getAdapterPosition() == headers.getSelectedPosition() ? 1 : 0]);
            }
        }
    }

    private final RequestListener<Drawable> mErrorListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
}
