package com.liskovsoft.smartyoutubetv2.common.misc;

import static org.junit.Assert.*;

import android.app.Activity;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowDialog;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
@LooperMode(LooperMode.Mode.PAUSED)
public class DialApprovalTest {
    private Activity activity;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        DialApproval.resume(activity);
    }

    @After
    public void cleanup() {
        DialApproval.cancel();
        DialApproval.pause(activity);
        activity.finish();
    }

    @Test
    public void noPairingBeforeExplicitApproval() {
        AtomicInteger count = new AtomicInteger();
        assertTrue(DialApproval.request("192.168.1.2", count::incrementAndGet));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, count.get());
        assertFalse(DialApproval.request("192.168.1.3", count::incrementAndGet));
        ((AlertDialog) ShadowDialog.getLatestDialog())
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, count.get());
    }

    @Test
    public void backgroundRequestIsRejected() {
        DialApproval.pause(activity);
        assertFalse(DialApproval.request("192.168.1.2", () -> fail("Background approval")));
    }

    @Test
    public void timeoutNeverPairs() {
        AtomicInteger count = new AtomicInteger();
        assertTrue(DialApproval.request("192.168.1.2", count::incrementAndGet));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(31));
        assertEquals(0, count.get());
        assertTrue(DialApproval.request("192.168.1.3", count::incrementAndGet));
    }

    @Test
    public void denialNeverPairs() {
        AtomicInteger count = new AtomicInteger();
        assertTrue(DialApproval.request("192.168.1.2", count::incrementAndGet));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        ((AlertDialog) ShadowDialog.getLatestDialog())
                .getButton(AlertDialog.BUTTON_NEGATIVE)
                .performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, count.get());
        assertTrue(DialApproval.request("192.168.1.3", count::incrementAndGet));
    }
}
