package com.liskovsoft.smartyoutubetv2.tv.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build.VERSION;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SearchOrbView.Colors;
import androidx.leanback.widget.SpeechOrbView;
import androidx.leanback.widget.VerticalGridView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.adapter.VideoGroupObjectAdapter;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.marqueetextview.MarqueeTextView;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.marqueetextviewcompat.MarqueeTextViewCompat;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.speedmarquee.SpeedMarquee;

public class ViewUtil {
    /**
     * Focused card zoom factor
     */
    public static final int FOCUS_ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL;
    //public static final int FOCUS_ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_NONE;
    /**
     * Dim focused card?
     */
    public static final boolean FOCUS_DIMMER_ENABLED = false;
    /**
     * Dim other rows in {@link RowPresenter}
     */
    public static final boolean ROW_SELECT_EFFECT_ENABLED = false;
    /**
     * Scroll continue threshold
     */
    public static final int GRID_SCROLL_CONTINUE_NUM = 10;
    public static final int ROW_SCROLL_CONTINUE_NUM = 4;

    /**
     * YouTube color scheme: flat cards, icon-only sidebar, no clock
     */
    public static boolean isYouTubeStyle(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(R.attr.youTubeStyle, value, true) && value.data != 0;
    }

    /**
     * YouTube color scheme has big video cards (264dp, like YouTube TV)
     */
    public static float getVideoCardScale(Context context) {
        return isYouTubeStyle(context) ? 1.32f : 1f;
    }

    /**
     * YouTube color scheme: cards of the selected row are 360dp wide, the rest 264dp
     */
    public static final float YOUTUBE_SELECTED_ROW_CARD_SCALE = 360f / 264;

    /**
     * YouTube TV marks the focused card with the outline only, without zooming it
     */
    public static int getFocusZoomFactor(Context context) {
        return isYouTubeStyle(context) ? FocusHighlight.ZOOM_FACTOR_NONE : FOCUS_ZOOM_FACTOR;
    }

    /**
     * Checks whether text is truncated (e.g. has ... at the end)
     */
    public static boolean isTruncated(TextView textView) {
        Layout layout = textView.getLayout();
        if (layout != null) {
            int lines = layout.getLineCount();
            if (lines > 0) {
                int ellipsisCount = layout.getEllipsisCount(lines - 1);
                if (ellipsisCount > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void disableMarquee(TextView... textViews) {
        if (VERSION.SDK_INT <= 19 || textViews == null) { // Android 4: Broken grid layout fix
            return;
        }

        for (TextView textView : textViews) {
            textView.setEllipsize(TruncateAt.END);
            // Line below cause broken grid layout on Android 4 and older
            textView.setHorizontallyScrolling(false);

            applyMarqueeRtlParams(textView, false);
        }
    }

    /**
     * <a href="https://stackoverflow.com/questions/3332924/textview-marquee-not-working">More info</a>
     */
    public static void enableMarquee(TextView... textViews) {
        if (VERSION.SDK_INT <= 19 || textViews == null) { // Android 4: Broken grid layout fix
            return;
        }

        for (TextView textView : textViews) {
            if (ViewUtil.isTruncated(textView)) { // multiline scroll fix
                textView.setEllipsize(TruncateAt.MARQUEE);
                textView.setMarqueeRepeatLimit(-1);
                textView.setHorizontallyScrolling(true);

                // App dialog title fix.
                //textView.setSelected(true);

                applyMarqueeRtlParams(textView, true);
            }
        }
    }

    public static void applyMarqueeRtlParams(TextView textView, boolean scroll) {
        if (!Helpers.isTextRTL(textView.getText())) {
            // TextView may be reused from rtl context. Do reset.
            // NOTE: don't enable commented options because Setting item's text won't be centered.
            //textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
            textView.setTextDirection(View.TEXT_DIRECTION_LTR);
            textView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            //textView.setGravity(Gravity.TOP | Gravity.START);
            return;
        }

        if (scroll) {
            // Fix: right scrolling on rtl languages
            // Fix: text disappear on rtl languages
            textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            textView.setTextDirection(View.TEXT_DIRECTION_RTL);
            textView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            textView.setGravity(Gravity.START);
        } else {
            // Fix: text disappear on rtl languages
            textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }
    }

    public static void setTextScrollSpeed(TextView textView, float speed) {
        if (VERSION.SDK_INT <= 19) { // Android 4: Broken grid layout fix
            return;
        }

        if (textView instanceof MarqueeTextViewCompat) {
            ((MarqueeTextViewCompat) textView).setMarqueeSpeedFactor(speed);
        } else if (textView instanceof MarqueeTextView) {
            ((MarqueeTextView) textView).setMarqueeSpeedFactor(speed);
        } else if (textView instanceof SpeedMarquee) {
            ((SpeedMarquee) textView).setSpeed(speed);
        }
    }

    public static void enableView(View view, boolean enabled) {
        if (view != null) {
            view.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
    }

    public static void setDimensions(View view, int width, int height) {
        if (view != null) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();

            if (lp != null) {
                if (width > 0) {
                    lp.width = width;
                }
                if (height > 0) {
                    lp.height = height;
                }
                view.setLayoutParams(lp);
            }
        }
    }

    public static boolean isListRowEmpty(Object obj) {
        if (obj instanceof ListRow) {
            ListRow row = (ListRow) obj;
            VideoGroupObjectAdapter adapter = (VideoGroupObjectAdapter) row.getAdapter();
            return adapter == null || adapter.isEmpty();
        }

        return true;
    }

    public static RequestOptions glideOptions() {
        return new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE) // ensure start animation from beginning
                .skipMemoryCache(true); // ensure start animation from beginning
    }

    public static void enableTransparentDialog(Context context, View rootView) {
        if (context == null || rootView == null || VERSION.SDK_INT <= 19) {
            return;
        }

        // Usually null. Present only on parent fragment.
        View mainContainer = rootView.findViewById(R.id.settings_preference_fragment_container);
        View mainFrame = rootView.findViewById(R.id.main_frame);
        View itemsContainer = rootView.findViewById(R.id.list);
        View title = rootView.findViewById(R.id.decor_title_container);
        int transparent = ContextCompat.getColor(context, R.color.transparent);
        int semiTransparent = ContextCompat.getColor(context, R.color.semi_grey);

        // Disable shadow outline on parent fragment
        if (mainContainer instanceof FrameLayout && VERSION.SDK_INT >= 21) {
            // ViewOutlineProvider: NoClassDefFoundError on API 19
            mainContainer.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        }
        if (mainFrame instanceof LinearLayout) {
            mainFrame.setBackgroundColor(transparent);
        }
        if (itemsContainer instanceof VerticalGridView) {
            // Set background for individual buttons in the list.
            // This is the only way to do this because items haven't been added yet to the container.
            ((VerticalGridView) itemsContainer).setOnChildLaidOutListener(
                    (parent, view, position, id) -> view.setBackgroundResource(R.drawable.transparent_dialog_item_bg)
            );
        }
        if (title instanceof FrameLayout) {
            title.setBackgroundColor(transparent);
            title.setVisibility(View.GONE);
        }
    }

    private static boolean sYouTubePaneDialog;

    /**
     * YouTube color scheme: the next dialog is opened from Settings and replaces the preview pane
     */
    public static void setYouTubePaneDialog(boolean enable) {
        sYouTubePaneDialog = enable;
    }

    public static boolean isYouTubePaneDialog() {
        return sYouTubePaneDialog && VERSION.SDK_INT >= 21;
    }

    /**
     * YouTube color scheme: dialog fills the Settings preview pane, title and options are centered like YouTube TV
     */
    public static void enableYouTubePaneDialog(Context context, View rootView) {
        if (!sYouTubePaneDialog || context == null || rootView == null || VERSION.SDK_INT < 21) {
            return;
        }

        // Usually null. Present only on parent fragment.
        View mainContainer = rootView.findViewById(R.id.settings_preference_fragment_container);
        if (mainContainer instanceof FrameLayout) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mainContainer.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.setMarginStart(context.getResources().getDimensionPixelSize(R.dimen.youtube_settings_pane_start));
            lp.topMargin = context.getResources().getDimensionPixelSize(R.dimen.youtube_settings_pane_top); // keep the title bar visible
            mainContainer.setLayoutParams(lp);
            mainContainer.setElevation(0);
        }

        View titleContainer = rootView.findViewById(R.id.decor_title_container);
        if (titleContainer != null) {
            titleContainer.setElevation(0);
        }

        View title = rootView.findViewById(R.id.decor_title);
        if (title instanceof TextView) {
            ((TextView) title).setGravity(Gravity.CENTER);
        }

        for (int id : new int[] {R.id.list, android.R.id.list}) {
            View list = rootView.findViewById(id);
            if (list == null) {
                continue;
            }

            ViewGroup.LayoutParams lp = list.getLayoutParams();
            lp.width = context.getResources().getDimensionPixelSize(R.dimen.youtube_settings_option_width);
            if (lp instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) lp).gravity = Gravity.CENTER_HORIZONTAL;
            } else if (lp instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) lp).gravity = Gravity.CENTER_HORIZONTAL;
            }
            list.setLayoutParams(lp);
        }
    }

    /**
     * YouTube color scheme: round mic button and a pill search field, no search and settings buttons
     */
    public static void applyYouTubeSearchBar(View bar, int micStart) {
        Context context = bar.getContext();
        Resources res = context.getResources();

        ((MarginLayoutParams) bar.getLayoutParams()).topMargin = res.getDimensionPixelSize(R.dimen.youtube_search_bar_top);
        bar.findViewById(R.id.lb_search_bar_search_orb).setVisibility(View.GONE);
        bar.findViewById(R.id.search_settings_orb).setVisibility(View.GONE);

        int buttonSize = res.getDimensionPixelSize(R.dimen.youtube_title_button_size);

        TextView editor = bar.findViewById(R.id.lb_search_text_editor);
        ((MarginLayoutParams) editor.getLayoutParams()).setMarginStart(res.getDimensionPixelSize(R.dimen.youtube_search_text_margin_start));
        editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        editor.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        int textColor = ContextCompat.getColor(context, R.color.youtube_sidebar_item_text);
        int hintColor = ContextCompat.getColor(context, R.color.scheme_youtube_secondary_text);
        editor.setTextColor(textColor);
        editor.setHintTextColor(hintColor);
        View searchField = bar.findViewById(R.id.lb_search_bar_items);
        searchField.setBackgroundResource(R.drawable.youtube_search_pill);
        MarginLayoutParams fieldParams = (MarginLayoutParams) searchField.getLayoutParams();
        fieldParams.height = buttonSize;
        fieldParams.setMarginStart(res.getDimensionPixelSize(R.dimen.youtube_search_field_margin_start));

        SpeechOrbView mic = bar.findViewById(R.id.lb_search_bar_speech_orb);
        ((MarginLayoutParams) mic.getLayoutParams()).setMarginStart(micStart);
        View micCircle = mic.findViewById(R.id.search_orb);
        micCircle.getLayoutParams().width = buttonSize;
        micCircle.getLayoutParams().height = buttonSize;
        Colors normal = new Colors(ContextCompat.getColor(context, R.color.scheme_youtube_thumbnail),
                ContextCompat.getColor(context, R.color.scheme_youtube_thumbnail), Color.WHITE);
        Colors focused = new Colors(Color.WHITE, Color.WHITE, ContextCompat.getColor(context, R.color.scheme_youtube_background));
        mic.setNotListeningOrbColors(normal);
        mic.setOrbColors(normal);
        mic.setOrbIcon(ContextCompat.getDrawable(context, R.drawable.youtube_mic));
        View.OnFocusChangeListener micListener = mic.getOnFocusChangeListener();
        mic.setOnFocusChangeListener((v, hasFocus) -> {
            if (micListener != null) {
                micListener.onFocusChange(v, hasFocus);
            }
            Colors colors = hasFocus ? focused : normal;
            mic.setNotListeningOrbColors(colors);
            mic.setOrbColors(colors);
            mic.setOrbIcon(ContextCompat.getDrawable(context, R.drawable.youtube_mic));
            // The bar dims the text on focus changes
            editor.setTextColor(textColor);
            editor.setHintTextColor(hintColor);
        });
    }

    public static void enableLeftDialog(Context context, View rootView) {
        if (context == null || rootView == null || VERSION.SDK_INT <= 19) {
            return;
        }

        // Usually null. Present only on parent fragment.
        View mainContainer = rootView.findViewById(R.id.settings_preference_fragment_container);

        if (mainContainer instanceof FrameLayout) {
            ((FrameLayout.LayoutParams) mainContainer.getLayoutParams()).gravity = Gravity.START;
        }
    }

    public static void makeMonochrome(ImageView iconView) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        iconView.setColorFilter(filter);
    }

    public static void setGravity(View view, int gravity) {
        if (view == null) {
            return;
        }

        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) lp;
            flp.gravity = gravity;
            view.setLayoutParams(flp);
        }
    }

    public static void setWidth(View view, int width) {
        if (view == null) {
            return;
        }

        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp != null) {
            lp.width = width;
            view.setLayoutParams(lp);
        }
    }

    public static void setPadding(View view, int padding) {
        if (view == null) {
            return;
        }

        view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
    }

    /**
     * Fix SDK 28+ GridLayoutManager broken navigation when using Japanese fonts
     */
    public static void fixApi28BrokenGridNavigation(TextView textView) {
        if (VERSION.SDK_INT >= 28) {
            // 1. Disable dynamic line spacing for special characters (prevents expansion for CJK glyphs)
            textView.setFallbackLineSpacing(false);
        }

        // 2. Remove system font padding to ensure consistent baseline and height
        textView.setIncludeFontPadding(false);

        // 3. Add fixed internal padding to create a "safe zone" for both Latin and Japanese text
        int paddingExtra = (int) (4 * textView.getResources().getDisplayMetrics().density);
        textView.setPadding(textView.getPaddingLeft(), paddingExtra, textView.getPaddingRight(), paddingExtra);
    }
}
