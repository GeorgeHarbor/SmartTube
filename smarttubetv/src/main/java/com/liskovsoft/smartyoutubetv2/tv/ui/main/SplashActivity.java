package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import com.liskovsoft.sharedutils.helpers.GlobalConstants;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.BrandedLoadingView;
import android.os.Bundle;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SplashPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;

public class SplashActivity extends MotherActivity implements SplashView {
    private static final String TAG = SplashActivity.class.getSimpleName();
    private Intent mNewIntent;
    private SplashPresenter mPresenter;

    @Override
    protected void initTheme() {
        // Keep the manifest theme's launch window visible while the app is starting.
        // The branded loading view supplies its own background after setContentView().
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(new BrandedLoadingView(this));

        mNewIntent = tagAssistantIntent(getIntent());

        mPresenter = SplashPresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();

        //finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mNewIntent = tagAssistantIntent(intent);

        mPresenter.onViewInitialized();
    }

    /**
     * Google Assistant: back from the player returns to the app (like ATV launcher intents), not to the home screen
     */
    private Intent tagAssistantIntent(Intent intent) {
        Uri referrer = Build.VERSION.SDK_INT >= 22 ? getReferrer() : null;

        if (intent != null && referrer != null && "com.google.android.katniss".equals(referrer.getHost())) {
            intent.putExtra(GlobalConstants.ATV_INTENT, true);
        }

        return intent;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.onViewDestroyed();
    }

    @Override
    public Intent getNewIntent() {
        return mNewIntent;
    }

    @Override
    public void finishView() {
        try {
            finish();
        } catch (NullPointerException e) {
            // NullPointerException: Attempt to invoke virtual method 'void com.android.server.wm.DisplayContent.moveStack(com.android.server.wm.TaskStack, boolean)'
            e.printStackTrace();
        }
    }
}
