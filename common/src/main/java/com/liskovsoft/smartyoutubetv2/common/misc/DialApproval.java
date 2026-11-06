package com.liskovsoft.smartyoutubetv2.common.misc;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;

import java.lang.ref.WeakReference;

/** The LAN peer cannot cause foregrounding or approval. Only the TV remote authorizes pairing. */
public final class DialApproval {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static WeakReference<Activity> foreground = new WeakReference<>(null);
    private static Request pending;

    private static final class Request {
        final Activity owner;
        final Runnable approved;
        final long expires = android.os.SystemClock.elapsedRealtime() + 30000;
        AlertDialog dialog;
        boolean done;

        Request(Activity owner, Runnable approved) {
            this.owner = owner;
            this.approved = approved;
        }
    }

    private DialApproval() {}

    public static synchronized void resume(Activity activity) {
        foreground = new WeakReference<>(activity);
    }

    public static synchronized void pause(Activity activity) {
        if (foreground.get() == activity) foreground.clear();
        if (pending != null && pending.owner == activity) cancel();
    }

    public static synchronized void cancel() {
        Request request = pending;
        pending = null;
        if (request != null) {
            request.done = true;
            MAIN.post(
                    () -> {
                        if (request.dialog != null) request.dialog.dismiss();
                    });
        }
    }

    public static synchronized boolean request(String address, Runnable approved) {
        Activity activity = foreground.get();
        if (activity == null || activity.isFinishing() || pending != null) return false;
        Request request = new Request(activity, approved);
        pending = request;
        MAIN.post(() -> show(request, address));
        MAIN.postDelayed(() -> expire(request), 30000);
        return true;
    }

    private static synchronized void expire(Request request) {
        if (pending == request) cancel();
    }

    private static synchronized void show(Request request, String address) {
        if (pending != request || foreground.get() != request.owner) {
            expire(request);
            return;
        }
        request.dialog =
                new AlertDialog.Builder(request.owner)
                        .setTitle("Allow a phone to cast?")
                        .setMessage(
                                "A device at "
                                        + address
                                        + " wants to control playback. Approve only if you just"
                                        + " started casting.")
                        .setNegativeButton("Deny", (dialog, which) -> expire(request))
                        .setPositiveButton("Allow", (dialog, which) -> approve(request))
                        .setOnCancelListener(dialog -> expire(request))
                        .create();
        try {
            request.dialog.show();
            request.dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
        } catch (RuntimeException failure) {
            expire(request);
        }
    }

    private static synchronized void approve(Request request) {
        if (pending != request
                || request.done
                || android.os.SystemClock.elapsedRealtime() >= request.expires
                || foreground.get() != request.owner) {
            expire(request);
            return;
        }
        request.done = true;
        pending = null;
        request.approved.run();
    }
}
