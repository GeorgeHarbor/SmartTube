package com.liskovsoft.smartyoutubetv2.common.misc;

import static org.junit.Assert.*;

import com.liskovsoft.sharedutils.helpers.Helpers;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class SettingsBackupTest {
    private File root;

    @Before
    public void setup() throws Exception {
        root = java.nio.file.Files.createTempDirectory("settings-backup").toFile();
    }

    @After
    public void cleanup() {
        SafeZip.delete(root);
    }

    private void put(File file, String value) throws Exception {
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String read(File file) throws Exception {
        return new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private String general(String settings, String master) {
        String[] values = new String[50];
        values[0] = "theme";
        values[33] = settings;
        values[41] = master;
        return Helpers.mergeData((Object[]) values);
    }

    @Test
    public void exportExcludesCredentialsPasswordsAndPairing() throws Exception {
        File files = new File(root, "files"), backup = new File(root, "backup");
        put(new File(files, "app_prefs/general_data"), general("settings-secret", "master-secret"));
        put(new File(files, "app_prefs/video_player_data"), "player-settings");
        put(new File(files, "app_prefs/accounts_data"), "account-secret");
        put(new File(files, "app_prefs/device_link_data"), "pairing-secret");
        put(new File(files, "global_prefs/media_service_account_data"), "refresh-secret");
        SettingsBackup.exportSettings(files, backup);
        String exported = read(new File(backup, "files/app_prefs/general_data"));
        assertFalse(exported.contains("settings-secret"));
        assertFalse(exported.contains("master-secret"));
        assertTrue(new File(backup, "files/app_prefs/video_player_data").isFile());
        assertFalse(new File(backup, "files/app_prefs/accounts_data").exists());
        assertFalse(new File(backup, "files/app_prefs/device_link_data").exists());
        assertFalse(new File(backup, "files/global_prefs/media_service_account_data").exists());
    }

    @Test
    public void restoreKeepsCurrentLocksAndCredentials() throws Exception {
        File files = new File(root, "files"), backup = new File(root, "backup");
        put(
                new File(files, "app_prefs/general_data"),
                general("local-settings-lock", "local-master-lock"));
        put(new File(files, "app_prefs/accounts_data"), "local-account-lock");
        put(new File(files, "global_prefs/media_service_account_data"), "local-token");
        put(
                new File(backup, "files/app_prefs/general_data"),
                general("imported-lock", "imported-master"));
        put(new File(backup, "files/app_prefs/video_player_data"), "new-player-settings");
        put(new File(backup, "files/global_prefs/media_service_account_data"), "attacker-token");
        SettingsBackup.restoreSettings(backup, files);
        String restored = read(new File(files, "app_prefs/general_data"));
        assertTrue(restored.contains("local-settings-lock"));
        assertTrue(restored.contains("local-master-lock"));
        assertFalse(restored.contains("imported-lock"));
        assertEquals(
                "local-token", read(new File(files, "global_prefs/media_service_account_data")));
        assertEquals("local-account-lock", read(new File(files, "app_prefs/accounts_data")));
        assertEquals("new-player-settings", read(new File(files, "app_prefs/video_player_data")));
    }
}
