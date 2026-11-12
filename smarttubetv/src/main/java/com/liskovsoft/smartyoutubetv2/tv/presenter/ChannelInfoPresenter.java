package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.os.Build.VERSION;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import com.bumptech.glide.Glide;
import com.liskovsoft.mediaserviceinterfaces.data.ChannelHeader;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * YouTube color scheme: channel page header (avatar, name, description, Play mix and Subscribe buttons)
 */
public class ChannelInfoPresenter extends RowPresenter {
    public interface Callback {
        void onPlayMixClicked();
        /**
         * @return new subscription state
         */
        boolean onSubscribeClicked();
    }

    public static class ChannelInfoRow extends Row {
        private final ChannelHeader mHeader;
        private final Callback mCallback;
        private boolean mIsSubscribed;

        public ChannelInfoRow(ChannelHeader header, Callback callback) {
            mHeader = header;
            mCallback = callback;
            mIsSubscribed = header.isSubscribed();
        }
    }

    public ChannelInfoPresenter() {
        setHeaderPresenter(null);
        setSelectEffectEnabled(false);
    }

    @Override
    protected ViewHolder createRowViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.youtube_channel_info, parent, false));
    }

    @Override
    protected void onBindRowViewHolder(ViewHolder vh, Object item) {
        super.onBindRowViewHolder(vh, item);

        ChannelInfoRow row = (ChannelInfoRow) item;
        ChannelHeader header = row.mHeader;
        View view = vh.view;
        Context context = view.getContext();

        ((TextView) view.findViewById(R.id.channel_title)).setText(header.getTitle());
        setTextOrHide(view.findViewById(R.id.channel_subtitle), header.getSubtitle());
        setTextOrHide(view.findViewById(R.id.channel_description), header.getDescription());

        ImageView avatar = view.findViewById(R.id.channel_avatar);
        if (VERSION.SDK_INT >= 21) {
            avatar.setClipToOutline(true); // round, the outline comes from the background
        }
        Glide.with(context)
                .load(header.getAvatarUrl())
                .apply(ViewUtil.glideOptions())
                .into(avatar);

        TextView playMix = view.findViewById(R.id.channel_play_mix);
        playMix.setVisibility(header.getMixVideoId() != null ? View.VISIBLE : View.GONE);
        // The icon follows the text color (dark on the white focused button)
        TextViewCompat.setCompoundDrawableTintList(playMix, ContextCompat.getColorStateList(context, R.color.youtube_channel_button_text));
        playMix.setOnClickListener(v -> row.mCallback.onPlayMixClicked());

        TextView subscribe = view.findViewById(R.id.channel_subscribe);
        subscribe.setVisibility(header.getChannelId() != null ? View.VISIBLE : View.GONE);
        subscribe.setText(row.mIsSubscribed ? R.string.channel_subscribed : R.string.action_subscribe);
        subscribe.setOnClickListener(v -> {
            row.mIsSubscribed = row.mCallback.onSubscribeClicked();
            subscribe.setText(row.mIsSubscribed ? R.string.channel_subscribed : R.string.action_subscribe);
        });
    }

    @Override
    protected void onUnbindRowViewHolder(ViewHolder vh) {
        super.onUnbindRowViewHolder(vh);

        Glide.with(vh.view.getContext().getApplicationContext()).clear((ImageView) vh.view.findViewById(R.id.channel_avatar));
    }

    private static void setTextOrHide(TextView view, CharSequence text) {
        view.setText(text);
        view.setVisibility(text != null ? View.VISIBLE : View.GONE);
    }
}
