package com.liskovsoft.smartyoutubetv2.common.utils;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.liskovsoft.sharedutils.helpers.KeyHelpers;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;

public class SimpleEditDialog {
    private static final java.util.concurrent.ExecutorService PASSWORD_WORKER = java.util.concurrent.Executors.newSingleThreadExecutor();
    public interface OnChange {
        boolean onChange(String newValue);
    }

    public static void show(Context context, String dialogTitle, String defaultValue, OnChange onChange) {
        show(context, dialogTitle, dialogTitle, defaultValue, onChange, null);
    }

    public static void show(Context context, String dialogTitle, String dialogHint, String defaultValue, OnChange onChange) {
        show(context, dialogTitle, dialogHint, defaultValue, onChange, null);
    }

    public static void show(Context context, String dialogTitle, String dialogHint, String defaultValue, OnChange onChange, Runnable onDismiss) {
        show(context, dialogTitle, dialogHint, defaultValue, onChange, onDismiss, false);
    }

    public static void showPassword(Context context, String dialogTitle, String defaultValue, OnChange onChange) {
        showPassword(context, dialogTitle, defaultValue, onChange, null);
    }

    public static void showPassword(Context context, String dialogTitle, String defaultValue, OnChange onChange, Runnable onDismiss) {
        show(context, dialogTitle, dialogTitle, defaultValue, onChange, onDismiss, true);
    }

    public static void showPasswordCheck(Context context, String title, String saved, OnChange success) {
        showPasswordCheck(context, title, saved, success, null);
    }
    public static void showPasswordCheck(Context context, String title, String saved, OnChange success, Runnable dismiss) {
        show(context, title, title, null, success, dismiss, true, saved);
    }
    private static void show(Context context, String dialogTitle, String dialogHint, String defaultValue, OnChange onChange, Runnable onDismiss, boolean isPassword) {
        show(context, dialogTitle, dialogHint, defaultValue, onChange, onDismiss, isPassword, null);
    }
    private static void show(Context context, String dialogTitle, String dialogHint, String defaultValue, OnChange onChange, Runnable onDismiss, boolean isPassword, String savedPassword) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppDialog);
        LayoutInflater inflater = LayoutInflater.from(context);
        View contentView = inflater.inflate(R.layout.simple_edit_dialog, null);

        EditText editField = contentView.findViewById(R.id.simple_edit_value);
        if (isPassword) {
            editField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        KeyHelpers.fixShowKeyboard(editField);

        editField.setText(defaultValue);
        editField.setHint(dialogHint);
        editField.setNextFocusDownId(android.R.id.button1); // OK button

        if (defaultValue != null) { // move cursor to the end
            editField.setSelection(defaultValue.length());
        }

        // keep empty, will override below.
        // https://stackoverflow.com/a/15619098/5379584
        AlertDialog configDialog = builder
                .setTitle(dialogTitle)
                .setView(contentView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> { })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> { })
                .create();

        if (onDismiss != null) {
            configDialog.setOnDismissListener(dialog -> onDismiss.run());
        }

        editField.setOnEditorActionListener((v, actionId, event) -> {
            switch (actionId) {
                case EditorInfo.IME_ACTION_NEXT:
                    configDialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
                    return true;
                case EditorInfo.IME_ACTION_DONE:
                    configDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                    return true;
            }
            return false;
        });

        try {
            configDialog.show();
        } catch (RuntimeException e) {
            // BadTokenException: Unable to add window -- token null is not for an application
            // RuntimeException: InputChannel is not initialized
            e.printStackTrace();
            MessageHelpers.showMessage(context, e.getMessage());
            return;
        }

        configDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view) -> {
            String newValue = editField.getText().toString();

            if (newValue.isEmpty()) {
                // Empty fields not allowed
                editField.setHint(R.string.enter_value);
                return;
            }

            if (!isPassword) {
                if (onChange.onChange(newValue)) configDialog.dismiss();
                return;
            }
            configDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            editField.setEnabled(false);
            PASSWORD_WORKER.execute(() -> {
                String encoded = null;
                try {
                    if (savedPassword == null || PasswordHash.matches(savedPassword, newValue))
                        encoded = PasswordHash.encoded(savedPassword) ? savedPassword : PasswordHash.hashInput(newValue);
                } catch (RuntimeException failure) { /* fail closed */ }
                final String result = encoded;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (!configDialog.isShowing()) return;
                    configDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    editField.setEnabled(true);
                    if (result != null && onChange.onChange(result)) configDialog.dismiss();
                    else editField.setError("Password not accepted");
                });
            });
        });

        configDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener((view) -> configDialog.dismiss());

        //editField.setNextFocusDownId(configDialog.getButton(AlertDialog.BUTTON_POSITIVE).getId()); // OK button
    }
}
