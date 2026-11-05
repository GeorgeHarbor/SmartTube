package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.browse;

import android.os.Bundle;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;

/**
 * YouTube color scheme: hosts the "Who's watching?" picker over the browse screen
 */
public class YouTubeAccountPickerActivity extends MotherActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new YouTubeAccountPicker(this).createView());

        // The sidebar behind collapses to the rail, as in YouTube
        BrowseView browseView = BrowsePresenter.instance(this).getView();
        if (browseView != null) {
            browseView.focusOnContent();
        }
    }

    @Override
    protected void initTheme() {
        // Keep the translucent manifest theme: the browse sidebar shows through
    }
}
