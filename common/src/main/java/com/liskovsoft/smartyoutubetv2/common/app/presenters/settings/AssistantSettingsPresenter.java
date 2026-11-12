package com.liskovsoft.smartyoutubetv2.common.app.presenters.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.AdbShell;
import com.liskovsoft.smartyoutubetv2.common.utils.AppDialogUtil;

import java.net.ConnectException;

public class AssistantSettingsPresenter extends BasePresenter<Void> {
    private static final String YOUTUBE_PKG_NAME = "com.google.android.youtube.tv";
    @SuppressLint("StaticFieldLeak")
    private static AssistantSettingsPresenter sInstance;

    public AssistantSettingsPresenter(Context context) {
        super(context);
    }

    public static AssistantSettingsPresenter instance(Context context) {
        if (sInstance == null) {
            sInstance = new AssistantSettingsPresenter(context);
        }

        sInstance.setContext(context);

        return sInstance;
    }

    public void unhold() {
        sInstance = null;
    }

    public void show() {
        AppDialogPresenter settingsPresenter = AppDialogPresenter.instance(getContext());

        settingsPresenter.appendSingleButton(UiOptionItem.from(getContext().getString(R.string.assistant_open_app),
                optionItem -> AppDialogUtil.showConfirmationDialog(getContext(),
                        getContext().getString(R.string.assistant_open_app_confirm), this::makeAssistantOpenApp)));

        settingsPresenter.showDialog(getContext().getString(R.string.settings_assistant), this::unhold);
    }

    /**
     * Assistant sends "play X on YouTube" to the YouTube app. With it disabled, Assistant falls back to a youtube.com link,
     * which Android 12+ hands only to an app the user approved for that domain.
     */
    private void makeAssistantOpenApp() {
        Context context = getContext().getApplicationContext();
        String command = "pm disable-user --user 0 " + YOUTUBE_PKG_NAME + (Build.VERSION.SDK_INT >= 31 ?
                "; pm set-app-links-user-selection --user 0 --package " + context.getPackageName() +
                        " true youtube.com www.youtube.com m.youtube.com youtu.be" : "");

        MessageHelpers.showLongMessage(context, R.string.assistant_allow_debugging);

        RxHelper.execute(RxHelper.fromCallable(() -> AdbShell.run(context, command)),
                output -> MessageHelpers.showLongMessage(context, output.contains("Error") || output.contains("Exception") ?
                        output : context.getString(R.string.assistant_open_app_done)),
                error -> MessageHelpers.showLongMessage(context, error instanceof ConnectException ?
                        context.getString(R.string.assistant_enable_network_debugging) : String.valueOf(error)));
    }
}
