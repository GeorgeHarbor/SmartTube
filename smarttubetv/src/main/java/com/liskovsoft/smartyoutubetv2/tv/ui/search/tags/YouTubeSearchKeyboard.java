package com.liskovsoft.smartyoutubetv2.tv.ui.search.tags;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.TextViewCompat;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * YouTube color scheme: search suggestions on the left, on-screen keyboard on the right (no system keyboard)
 */
public class YouTubeSearchKeyboard {
    private static final String[] LETTERS = {"abcdefg", "hijklmn", "opqrstu", "vwxyz-'"};
    private static final String[] SYMBOLS = {"1234567", "890&#()", "@!?:~\",", "._/+$%*"};
    private static final int MAX_SUGGESTIONS = 10;
    private static final int MAX_SUGGESTIONS_WITH_RESULTS = 5;

    public interface Callback {
        String getText();
        void setText(String text);
        void onSubmit(String text);
        void onSuggestionClicked(Tag tag);
        void onSuggestionLongClicked(Tag tag);
        void onSettingsClicked();
    }

    private final Context mContext;
    private final Callback mCallback;
    private final LinearLayout mRoot;
    private final LinearLayout mSuggestions;
    private final List<TextView> mKeys = new ArrayList<>();
    private final List<Tag> mTags = new ArrayList<>();
    private final ColorStateList mTextColor;
    private TextView mSymbolsKey;
    private boolean mIsSymbols;
    private int mMaxSuggestions = MAX_SUGGESTIONS;

    public YouTubeSearchKeyboard(Context context, Callback callback) {
        mContext = context;
        mCallback = callback;
        mTextColor = ContextCompat.getColorStateList(context, R.color.youtube_key_text);

        mRoot = new LinearLayout(context);
        mRoot.setOrientation(LinearLayout.HORIZONTAL);

        mSuggestions = new LinearLayout(context);
        mSuggestions.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams suggestionsParams = new LinearLayout.LayoutParams(dp(264), ViewGroup.LayoutParams.WRAP_CONTENT);
        suggestionsParams.setMarginStart(dp(82.5f));
        mRoot.addView(mSuggestions, suggestionsParams);

        LinearLayout keyboard = new LinearLayout(context);
        keyboard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams keyboardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        keyboardParams.setMarginStart(dp(10.5f));
        mRoot.addView(keyboard, keyboardParams);

        for (int row = 0; row < LETTERS.length; row++) {
            LinearLayout keyRow = new LinearLayout(context);
            for (int i = 0; i < LETTERS[row].length(); i++) {
                TextView key = createKey();
                key.setOnClickListener(v -> append(((TextView) v).getText().toString().toLowerCase()));
                mKeys.add(key);
                keyRow.addView(key, keyParams(dp(27), 0));
            }
            keyRow.addView(createSpecialKey(row), keyParams(dp(40), dp(4)));
            keyboard.addView(keyRow);
        }

        LinearLayout buttons = new LinearLayout(context);
        buttons.setPadding(dp(7.5f), 0, 0, 0);
        buttons.addView(createButton(R.string.youtube_key_space, v -> append(" ")), keyParams(dp(66), 0));
        buttons.addView(createButton(R.string.youtube_key_clear, v -> mCallback.setText("")), keyParams(dp(66), 0));
        buttons.addView(createButton(R.string.action_search, v -> mCallback.onSubmit(mCallback.getText())), keyParams(dp(66), 0));
        keyboard.addView(buttons);

        updateKeyLabels();
    }

    public View getView() {
        return mRoot;
    }

    public void focus() {
        mKeys.get(0).requestFocus();
    }

    /**
     * Focus the item right above the results (YouTube moves up from the results this way)
     */
    public void focusBottom() {
        int count = mSuggestions.getChildCount();
        if (count > 0) {
            mSuggestions.getChildAt(count - 1).requestFocus();
        } else {
            focus();
        }
    }

    public void setSuggestions(List<Tag> tags) {
        mTags.clear();
        if (tags != null) {
            mTags.addAll(tags);
        }
        updateSuggestions();
    }

    public void removeSuggestion(Tag tag) {
        mTags.remove(tag);
        updateSuggestions();
    }

    /**
     * The list gets shorter when the results are shown below it
     */
    public void setHasResults(boolean hasResults) {
        int max = hasResults ? MAX_SUGGESTIONS_WITH_RESULTS : MAX_SUGGESTIONS;
        if (max != mMaxSuggestions) {
            mMaxSuggestions = max;
            updateSuggestions();
        }
    }

    private void updateSuggestions() {
        View focused = mSuggestions.findFocus();
        int focusedIndex = focused != null ? mSuggestions.indexOfChild(focused) : -1;
        mSuggestions.removeAllViews();
        String query = mCallback.getText();

        for (int i = 0; i < Math.min(mTags.size(), mMaxSuggestions); i++) {
            Tag tag = mTags.get(i);
            if (tag == null || tag.tag == null) {
                continue;
            }
            mSuggestions.addView(createSuggestion(tag, query), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(27)));
            ((LinearLayout.LayoutParams) mSuggestions.getChildAt(mSuggestions.getChildCount() - 1).getLayoutParams()).setMargins(0, dp(3), 0, dp(3));
        }

        // Don't lose focus when the list is refreshed under it
        if (focusedIndex != -1) {
            int count = mSuggestions.getChildCount();
            if (count > 0) {
                mSuggestions.getChildAt(Math.min(focusedIndex, count - 1)).requestFocus();
            } else {
                focus();
            }
        }
    }

    private TextView createSuggestion(Tag tag, String query) {
        TextView view = new TextView(mContext);
        view.setFocusable(true);
        view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(4), 0, dp(8), 0);
        view.setBackgroundResource(R.drawable.youtube_search_suggestion);
        view.setTextColor(ContextCompat.getColorStateList(mContext, R.color.youtube_sidebar_item_text));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.youtube_search_icon, 0, 0, 0);
        view.setCompoundDrawablePadding(dp(7));
        TextViewCompat.setCompoundDrawableTintList(view, view.getTextColors());

        // The typed part is regular, the completion is bold
        SpannableString text = new SpannableString(tag.tag);
        int start = query != null && tag.tag.toLowerCase().startsWith(query.toLowerCase()) ? query.length() : 0;
        text.setSpan(new StyleSpan(Typeface.BOLD), start, tag.tag.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(text);

        view.setOnClickListener(v -> mCallback.onSuggestionClicked(tag));
        view.setOnLongClickListener(v -> {
            mCallback.onSuggestionLongClicked(tag);
            return true;
        });
        return view;
    }

    private TextView createKey() {
        TextView key = new TextView(mContext);
        key.setFocusable(true);
        key.setGravity(Gravity.CENTER);
        key.setBackgroundResource(R.drawable.youtube_key);
        key.setTextColor(mTextColor);
        key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        return key;
    }

    /**
     * Right column: backspace, &123, globe (search settings)
     */
    private View createSpecialKey(int row) {
        switch (row) {
            case 0:
                return createIconKey(R.drawable.youtube_backspace, v -> {
                    String text = mCallback.getText();
                    if (!text.isEmpty()) {
                        mCallback.setText(text.substring(0, text.length() - 1));
                    }
                });
            case 1:
                mSymbolsKey = createKey();
                mSymbolsKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                mSymbolsKey.setTypeface(Typeface.DEFAULT_BOLD);
                mSymbolsKey.setOnClickListener(v -> {
                    mIsSymbols = !mIsSymbols;
                    updateKeyLabels();
                });
                return mSymbolsKey;
            case 2:
                return createIconKey(R.drawable.youtube_globe, v -> mCallback.onSettingsClicked());
            default:
                View empty = new View(mContext);
                empty.setFocusable(false);
                return empty;
        }
    }

    private ImageView createIconKey(int iconResId, View.OnClickListener listener) {
        ImageView key = new ImageView(mContext);
        key.setFocusable(true);
        key.setScaleType(ImageView.ScaleType.CENTER);
        key.setBackgroundResource(R.drawable.youtube_key);
        key.setImageResource(iconResId);
        ImageViewCompat.setImageTintList(key, mTextColor);
        key.setOnClickListener(listener);
        return key;
    }

    private TextView createButton(int textResId, View.OnClickListener listener) {
        TextView button = new TextView(mContext);
        button.setFocusable(true);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.youtube_key_button);
        button.setTextColor(mTextColor);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(true);
        button.setText(textResId);
        button.setOnClickListener(listener);
        return button;
    }

    private void updateKeyLabels() {
        String[] rows = mIsSymbols ? SYMBOLS : LETTERS;
        for (int i = 0; i < mKeys.size(); i++) {
            mKeys.get(i).setText(String.valueOf(rows[i / 7].charAt(i % 7)).toUpperCase());
        }
        mSymbolsKey.setText(mIsSymbols ? "ABC" : "&123");
    }

    private void append(String text) {
        mCallback.setText(mCallback.getText() + text);
    }

    private LinearLayout.LayoutParams keyParams(int width, int extraMarginStart) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, dp(27));
        params.setMargins(dp(3) + extraMarginStart, dp(3), dp(3), dp(3));
        return params;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, mContext.getResources().getDisplayMetrics());
    }
}
