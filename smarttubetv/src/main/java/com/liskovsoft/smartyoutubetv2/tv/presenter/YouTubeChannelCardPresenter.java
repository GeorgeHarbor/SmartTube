package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.os.Build.VERSION;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * YouTube color scheme: round channel pictures with the centered name, handle and subscribers below
 */
public class YouTubeChannelCardPresenter extends VideoCardPresenter {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ViewHolder vh = super.onCreateViewHolder(parent);
        View cardView = vh.view;
        Context context = cardView.getContext();

        if (VERSION.SDK_INT >= 21) {
            for (int id : new int[] {R.id.main_image, R.id.preview_container}) {
                View picture = cardView.findViewById(id);
                if (picture != null) {
                    picture.setBackground(ContextCompat.getDrawable(context, R.drawable.youtube_channel_avatar));
                }
            }
        }

        if (VERSION.SDK_INT >= 23) {
            cardView.findViewById(R.id.main_image_wrapper).setForeground(ContextCompat.getDrawable(context, R.drawable.youtube_channel_card_focus));
        }

        for (int id : new int[] {R.id.title_text, R.id.content_text}) {
            TextView text = cardView.findViewById(id);
            if (text != null) {
                text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); // the marquee text ignores gravity
            }
        }

        return vh;
    }

    @Override
    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        int size = context.getResources().getDimensionPixelSize(R.dimen.youtube_channel_card_size);
        return new Pair<>(size, size);
    }

    @Override
    protected int getCardPreviewType(Context context) {
        return MainUIData.CARD_PREVIEW_DISABLED; // channels don't have previews
    }
}
