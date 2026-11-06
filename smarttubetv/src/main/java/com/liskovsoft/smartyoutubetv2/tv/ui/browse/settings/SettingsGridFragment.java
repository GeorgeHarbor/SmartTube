package com.liskovsoft.smartyoutubetv2.tv.ui.browse.settings;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.leanback.widget.FocusHighlight;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.SimpleEditDialog;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.SettingsCardPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.interfaces.SettingsSection;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.UriBackgroundManager;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.fragments.GridFragment;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;

public class SettingsGridFragment extends GridFragment implements SettingsSection {
    private static final String TAG = SettingsGridFragment.class.getSimpleName();
    private ArrayObjectAdapter mSettingsAdapter;
    private BrowsePresenter mMainPresenter;
    private UriBackgroundManager mBackgroundManager;
    private final List<SettingsGroup> mPendingUpdates = new ArrayList<>();
    private View mPane;
    // Options preview. Presenters normally open their dialog right away, Accounts after the avatars are loaded.
    private Object mSelectedItem;
    private boolean mIsPreviewRun;
    private boolean mIsPreviewShown;
    private boolean mIsOpening;
    private int mPendingPreviews;
    private Object mPendingItem;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMainPresenter = BrowsePresenter.instance(getContext());
        mBackgroundManager = ((LeanbackActivity) getActivity()).getBackgroundManager();

        setupAdapter();
        setupEventListeners();
        applyPendingUpdates();

        if (getMainFragmentAdapter().getFragmentHost() != null) {
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }
    }

    @Override
    protected void showOrHideTitle() {
        // NOP. Always show Browse fragment title
    }

    private void applyPendingUpdates() {
        for (SettingsGroup group : mPendingUpdates) {
            update(group);
        }

        mPendingUpdates.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        if (ViewUtil.isYouTubeStyle(getContext())) {
            // YouTube TV Settings: text list on the left, preview of the selected item on the right
            mPane = inflater.inflate(R.layout.youtube_settings_pane, (ViewGroup) root, false);
            ((ViewGroup) root).addView(mPane, 0);
        }

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mPane != null) {
            getBrowseGrid().setVerticalSpacing(0); // list items touch, like YouTube
            AppDialogPresenter.instance(getContext()).setDialogPreview(this::onPreview);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPane != null) {
            updatePane(mSelectedItem); // values could be changed in the dialog
        }
    }

    /**
     * The settings dialog opens over the preview, in the same place (the preview stays to avoid a blank frame)
     */
    private void openSettings(SettingsItem item) {
        ViewUtil.setYouTubePaneDialog(mPane != null);
        mIsOpening = true;
        item.onClick.run();
        mIsOpening = false;
    }

    private boolean onPreview(CharSequence title, List<OptionCategory> categories) {
        if (mIsOpening) {
            return false; // the user opens the dialog
        }

        if (mIsPreviewRun) {
            mIsPreviewShown = true;
            showOptions(title, categories);
            return true;
        }

        if (mPendingPreviews > 0) { // delayed preview, show it only if its item is still selected
            mPendingPreviews--;
            if (mPendingItem == mSelectedItem && mPane != null) {
                showOptions(title, categories);
            }
            return true;
        }

        return false; // not ours
    }

    private void showOptions(CharSequence title, List<OptionCategory> categories) {
        ((TextView) mPane.findViewById(R.id.settings_pane_title)).setText(title);
        ViewGroup options = mPane.findViewById(R.id.settings_pane_options);
        options.removeAllViews();

        if (categories == null) {
            return;
        }

        // Same rows as in the dialog
        LayoutInflater inflater = LayoutInflater.from(new ContextThemeWrapper(getContext(), R.style.App_Theme_YouTube_Preferences));

        for (OptionCategory category : categories) {
            boolean isSingle = category.type == OptionCategory.TYPE_SINGLE_SWITCH || category.type == OptionCategory.TYPE_SINGLE_BUTTON;
            OptionItem option = isSingle && category.options != null && !category.options.isEmpty() ? category.options.get(0) : null;
            CharSequence rowTitle = option != null ? option.getTitle() : category.title;

            if (rowTitle == null) {
                continue;
            }

            View row = inflater.inflate(R.layout.leanback_preference, options, false);
            row.setFocusable(false);
            ((TextView) row.findViewById(android.R.id.title)).setText(rowTitle);
            row.findViewById(android.R.id.summary).setVisibility(View.GONE);
            row.findViewById(R.id.icon_frame).setVisibility(View.GONE);
            ViewGroup widgetFrame = row.findViewById(android.R.id.widget_frame);

            if (category.type == OptionCategory.TYPE_SINGLE_SWITCH && option != null) {
                CheckBox checkBox = (CheckBox) inflater.inflate(R.layout.preference_widget_checkbox, widgetFrame, false);
                checkBox.setChecked(option.isSelected());
                widgetFrame.addView(checkBox);
            } else {
                widgetFrame.setVisibility(View.GONE);
            }

            options.addView(row);
        }
    }

    @Override
    public void onDestroyView() {
        if (mPane != null) {
            AppDialogPresenter.instance(getContext()).setDialogPreview(null);
        }
        mPane = null;
        super.onDestroyView();
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener((itemViewHolder, item, rowViewHolder, row) -> updatePane(item));
    }

    /**
     * Options of the selected item are shown right away, RIGHT/OK moves into them (opens the dialog in the same place)
     */
    private void updatePane(Object item) {
        mSelectedItem = item;

        if (mPane == null || !(item instanceof SettingsItem)) {
            return;
        }

        SettingsItem settingsItem = (SettingsItem) item;

        if (getGeneralData().getSettingsPassword() != null) { // don't reveal protected settings
            showOptions(settingsItem.title, null);
            return;
        }

        mIsPreviewShown = false;
        mIsPreviewRun = true;
        settingsItem.onClick.run();
        mIsPreviewRun = false;

        if (!mIsPreviewShown) {
            showOptions(settingsItem.title, null); // still loading
            mPendingPreviews++;
            mPendingItem = item;
        }
    }

    private void setupAdapter() {
        if (ViewUtil.isYouTubeStyle(getContext())) {
            VerticalGridPresenter presenter = new VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_NONE, false);
            presenter.setShadowEnabled(false);
            presenter.setNumberOfColumns(1);
            setGridPresenter(presenter);

            if (mSettingsAdapter == null) {
                mSettingsAdapter = new ArrayObjectAdapter(new YouTubeSettingsItemPresenter());
                setAdapter(mSettingsAdapter);
            }
            return;
        }

        VerticalGridPresenter presenter = new VerticalGridPresenter(ViewUtil.FOCUS_ZOOM_FACTOR, ViewUtil.FOCUS_DIMMER_ENABLED);
        presenter.enableChildRoundedCorners(getMainUIData().isUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS));
        presenter.setNumberOfColumns(GridFragmentHelper.getMaxColsNum(getContext(), R.dimen.settings_card_width));
        setGridPresenter(presenter);

        if (mSettingsAdapter == null) {
            SettingsCardPresenter gridPresenter = new SettingsCardPresenter();
            mSettingsAdapter = new ArrayObjectAdapter(gridPresenter);
            setAdapter(mSettingsAdapter);
        }
    }

    @Override
    public void clear() {
        if (mSettingsAdapter != null) {
            mSettingsAdapter.clear();
        }
    }

    @Override
    public boolean isEmpty() {
        if (mSettingsAdapter == null) {
            return mPendingUpdates.isEmpty();
        }

        return mSettingsAdapter.size() == 0;
    }

    @Override
    public void update(SettingsGroup group) {
        if (mSettingsAdapter == null) {
            mPendingUpdates.add(group);
            return;
        }

        // Always clear (continuation not supported)
        clear();

        if (group != null) {
            for (SettingsItem item : group.getItems()) {
                mSettingsAdapter.add(item);
            }
        }
    }

    private MainUIData getMainUIData() {
        return MainUIData.instance(getContext());
    }

    private GeneralData getGeneralData() {
        return GeneralData.instance(getContext());
    }

    private static final class YouTubeSettingsItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.youtube_settings_item, parent, false);
            // Go right into the options of the item, like YouTube
            view.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.getAction() == KeyEvent.ACTION_DOWN) {
                    v.performClick();
                    return true;
                }
                return false;
            });
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText(((SettingsItem) item).title);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
            // NOP
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof SettingsItem) {
                String password = getGeneralData().getSettingsPassword();

                if (password == null) {
                    openSettings((SettingsItem) item);
                } else {
                    SimpleEditDialog.showPasswordCheck(
                            getContext(),
                            getContext().getString(R.string.enter_settings_password),
                            password,
                            newValue -> {
                                if (newValue != null) {
                                    getGeneralData().setSettingsPassword(newValue);
                                    openSettings((SettingsItem) item);
                                    return true;
                                }
                                return false;
                            });
                }

                // Close PIP inside Settings section
                //if (PlaybackPresenter.instance(getContext()).isInPipMode()) {
                //    PlaybackPresenter.instance(getContext()).forceFinish();
                //}
            } else {
                Toast.makeText(getContext(), item.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
