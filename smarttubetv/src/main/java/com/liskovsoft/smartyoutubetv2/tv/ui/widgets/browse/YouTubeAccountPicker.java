package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.browse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.YTSignInPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AccountSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * YouTube color scheme: full screen "Who's watching?" picker, the closed sidebar stays visible on the left
 */
public class YouTubeAccountPicker {
    // Letter avatars of accounts without a picture
    private static final int[] LETTER_COLORS = {0xFF5E4039, 0xFF2F431E, 0xFF3D3F6B, 0xFF6B3D5A, 0xFF2E5A5A};
    private final Activity mActivity;
    private final List<View[]> mColumns = new ArrayList<>(); // name, email, gear of every account

    YouTubeAccountPicker(Activity activity) {
        mActivity = activity;
    }

    public static void show(Context context) {
        Intent intent = new Intent(context, YouTubeAccountPickerActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    View createView() {
        List<Account> accounts = new ArrayList<>();
        MediaServiceManager.instance().loadAccounts(list -> {
            if (list != null) {
                accounts.addAll(list);
            }
        });

        FrameLayout root = new FrameLayout(mActivity);

        // The sidebar rail stays uncovered
        View background = new View(mActivity);
        background.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.scheme_youtube_background));
        FrameLayout.LayoutParams backgroundParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        backgroundParams.setMarginStart(mActivity.getResources().getDimensionPixelSize(R.dimen.youtube_sidebar_rail_width));
        root.addView(background, backgroundParams);

        TextView title = createText(R.string.youtube_whos_watching, 29, Color.WHITE);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, topParams(ViewGroup.LayoutParams.MATCH_PARENT, 86.5f));

        LinearLayout row = new LinearLayout(mActivity);
        View focused = null;
        for (Account account : accounts) {
            View circle = addColumn(row, account);
            if (account.isSelected()) {
                focused = circle;
            }
        }
        addColumn(row, null); // add account
        root.addView(row, topParams(ViewGroup.LayoutParams.WRAP_CONTENT, 152));

        TextView guest = createText(R.string.youtube_watch_as_guest, 12.5f, ContextCompat.getColor(mActivity, R.color.youtube_sidebar_item_text));
        guest.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        guest.setGravity(Gravity.CENTER);
        guest.setPadding(dp(18), 0, dp(18), 0);
        guest.setBackgroundResource(R.drawable.youtube_search_pill);
        guest.setFocusable(true);
        guest.setOnClickListener(v -> select(null));
        FrameLayout.LayoutParams guestParams = topParams(ViewGroup.LayoutParams.WRAP_CONTENT, 444);
        guestParams.height = dp(36);
        root.addView(guest, guestParams);

        (focused != null ? focused : guest).requestFocus();

        return root;
    }

    /**
     * Circle, name and the email with the settings button under the focused account
     */
    private View addColumn(LinearLayout row, Account account) {
        LinearLayout column = new LinearLayout(mActivity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        row.addView(column, new LinearLayout.LayoutParams(dp(180), ViewGroup.LayoutParams.WRAP_CONTENT));

        // Focus ring is flush with the avatar
        FrameLayout circle = new FrameLayout(mActivity);
        circle.setFocusable(true);
        circle.setPadding(dp(4), dp(4), dp(4), dp(4));
        circle.setForeground(ContextCompat.getDrawable(mActivity, R.drawable.youtube_account_ring));
        column.addView(circle, new LinearLayout.LayoutParams(dp(156), dp(156)));

        String name = account == null ? mActivity.getString(R.string.youtube_add_account)
                : (account.getName() != null ? account.getName() : account.getEmail());

        GradientDrawable avatarBackground = new GradientDrawable();
        avatarBackground.setShape(GradientDrawable.OVAL);
        TextView letter = new TextView(mActivity);
        letter.setGravity(Gravity.CENTER);
        letter.setTextColor(Color.WHITE);
        letter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 80);
        letter.setIncludeFontPadding(false);
        letter.setBackground(avatarBackground);
        circle.addView(letter, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (account == null) {
            avatarBackground.setColor(0xFF1D1D1D);
            ImageView plus = new ImageView(mActivity);
            plus.setImageResource(R.drawable.youtube_plus);
            plus.setScaleType(ImageView.ScaleType.CENTER);
            circle.addView(plus);
            circle.setOnClickListener(v -> {
                mActivity.finish();
                YTSignInPresenter.instance(mActivity).start();
            });
        } else {
            avatarBackground.setColor(LETTER_COLORS[Math.abs(String.valueOf(name).hashCode()) % LETTER_COLORS.length]);
            letter.setText(TextUtils.isEmpty(name) ? "" : name.substring(0, 1).toUpperCase());
            if (account.getAvatarImageUrl() != null) {
                ImageView avatar = new ImageView(mActivity);
                circle.addView(avatar);
                Glide.with(mActivity).load(account.getAvatarImageUrl()).apply(RequestOptions.circleCropTransform()).into(avatar);
            }
            circle.setOnClickListener(v -> select(account));
        }

        TextView nameView = createText(0, 18, ContextCompat.getColor(mActivity, R.color.scheme_youtube_secondary_text));
        nameView.setText(name);
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(9.5f);
        column.addView(nameView, nameParams);

        TextView email = createText(0, 12.5f, ContextCompat.getColor(mActivity, R.color.scheme_youtube_secondary_text));
        email.setText(account != null ? account.getEmail() : null);
        email.setSingleLine(true);
        email.setEllipsize(TextUtils.TruncateAt.END);
        email.setGravity(Gravity.CENTER_HORIZONTAL);
        email.setVisibility(View.GONE);
        LinearLayout.LayoutParams emailParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        emailParams.topMargin = dp(5.5f);
        column.addView(email, emailParams);

        ImageView gear = new ImageView(mActivity);
        gear.setFocusable(true);
        gear.setScaleType(ImageView.ScaleType.CENTER);
        gear.setImageResource(R.drawable.youtube_ic_settings);
        gear.setBackgroundResource(R.drawable.youtube_search_pill);
        ImageViewCompat.setImageTintList(gear, ContextCompat.getColorStateList(mActivity, R.color.youtube_sidebar_item_text));
        gear.setVisibility(View.INVISIBLE);
        gear.setOnClickListener(v -> {
            mActivity.finish();
            AccountSettingsPresenter.instance(mActivity).show();
        });
        LinearLayout.LayoutParams gearParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        gearParams.topMargin = dp(20);
        column.addView(gear, gearParams);

        View[] views = {nameView, email, gear};
        mColumns.add(views);
        circle.setOnFocusChangeListener((v, hasFocus) -> {
            nameView.setTextColor(ContextCompat.getColor(mActivity, hasFocus ? R.color.white : R.color.scheme_youtube_secondary_text));
            if (hasFocus) {
                activate(views, account != null);
            }
        });

        return circle;
    }

    /**
     * The email and the settings button follow the focused account
     */
    private void activate(View[] active, boolean isAccount) {
        for (View[] views : mColumns) {
            boolean show = views == active && isAccount;
            views[1].setVisibility(show && !TextUtils.isEmpty(((TextView) views[1]).getText()) ? View.VISIBLE : View.GONE);
            views[2].setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void select(Account account) {
        mActivity.finish();
        AccountSelectionPresenter.instance(mActivity).selectAccount(account);
    }

    private TextView createText(int textResId, float sizeSp, int color) {
        TextView text = new TextView(mActivity);
        if (textResId != 0) {
            text.setText(textResId);
        }
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        text.setTextColor(color);
        text.setIncludeFontPadding(false);
        return text;
    }

    private FrameLayout.LayoutParams topParams(int width, float topDp) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        params.topMargin = dp(topDp);
        return params;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, mActivity.getResources().getDisplayMetrics());
    }
}
