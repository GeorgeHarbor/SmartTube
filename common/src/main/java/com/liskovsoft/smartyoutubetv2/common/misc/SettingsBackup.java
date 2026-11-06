package com.liskovsoft.smartyoutubetv2.common.misc;

import com.liskovsoft.sharedutils.helpers.Helpers;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Explicit settings schema. Unknown files, account records and shared-preference XML are never
 * restored.
 */
public final class SettingsBackup {
    private static final Set<String> ALLOWED =
            new HashSet<>(
                    Arrays.asList(
                            "general_data",
                            "video_player_data",
                            "main_ui_data2",
                            "search_data",
                            "DeArrowData",
                            "content_block_data",
                            "video_player_tweaks_data",
                            "blocked_channel_data",
                            "channel_group_data",
                            "sidebar_data",
                            "view_manager_data"));

    private SettingsBackup() {}

    static boolean allowed(String name) {
        for (String key : ALLOWED) if (name.equals(key) || name.endsWith("_" + key)) return true;
        return false;
    }

    private static String sanitize(String name, String value) {
        if (name.equals("general_data") || name.endsWith("_general_data")) {
            String[] parts = Helpers.splitData(value);
            if (parts == null) return "";
            if (parts.length > 33) parts[33] = null;
            if (parts.length > 41) parts[41] = null;
            return Helpers.mergeData((Object[]) parts);
        }
        return value;
    }

    public static void exportSettings(File appFiles, File destination) throws IOException {
        SafeZip.delete(destination);
        File output = new File(destination, "files/app_prefs");
        if (!output.mkdirs()) throw new IOException("Cannot create backup directory");
        copyAllowed(new File(appFiles, "app_prefs"), output, false);
        try (FileOutputStream out =
                new FileOutputStream(new File(destination, "settings-backup-version"))) {
            out.write("1".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void copyAllowed(File source, File target, boolean restoring)
            throws IOException {
        File[] files = source.listFiles();
        if (files == null) return;
        for (File file : files)
            if (file.isFile() && allowed(file.getName())) {
                if (file.length() > 5L * 1024 * 1024)
                    throw new IOException("Settings file too large");
                String data = new String(read(file), StandardCharsets.UTF_8);
                if (!target.mkdirs() && !target.isDirectory())
                    throw new IOException("Cannot create settings directory");
                android.util.AtomicFile atomic =
                        new android.util.AtomicFile(new File(target, file.getName()));
                FileOutputStream out = null;
                try {
                    String clean = sanitize(file.getName(), data);
                    if (restoring
                            && (file.getName().equals("general_data")
                                    || file.getName().endsWith("_general_data"))) {
                        File local = new File(target, file.getName());
                        String[] incoming = Helpers.splitData(clean);
                        String[] existing =
                                local.isFile()
                                        ? Helpers.splitData(
                                                new String(read(local), StandardCharsets.UTF_8))
                                        : null;
                        if (incoming != null) {
                            if (incoming.length > 33)
                                incoming[33] =
                                        existing != null && existing.length > 33
                                                ? existing[33]
                                                : null;
                            if (incoming.length > 41)
                                incoming[41] =
                                        existing != null && existing.length > 41
                                                ? existing[41]
                                                : null;
                            clean = Helpers.mergeData((Object[]) incoming);
                        }
                    }
                    out = atomic.startWrite();
                    out.write(clean.getBytes(StandardCharsets.UTF_8));
                    atomic.finishWrite(out);
                } catch (IOException e) {
                    if (out != null) atomic.failWrite(out);
                    throw e;
                }
            }
    }

    private static byte[] read(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > 5 * 1024 * 1024) throw new IOException("Settings too large");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    public static void restoreSettings(File backup, File appFiles) throws IOException {
        File approved = new File(backup, "files/app_prefs");
        File version = new File(backup,"settings-backup-version");
        if(version.exists() && !"1".equals(new String(read(version),StandardCharsets.UTF_8).trim()))
            throw new IOException("Unsupported settings backup version");
        if(!approved.isDirectory() || approved.listFiles()==null || approved.listFiles().length==0)
            throw new IOException("No compatible settings in this backup");
        File live = new File(appFiles, "app_prefs");
        File prepared = new File(appFiles, ".validated-settings-" + java.util.UUID.randomUUID());
        File previous = new File(appFiles, ".previous-settings");
        if (previous.exists()) {
            if (!live.exists() && !previous.renameTo(live))
                throw new IOException("Cannot recover previous settings");
            if (live.exists()) SafeZip.delete(previous);
        }
        try {
            if (!prepared.mkdirs()) throw new IOException("Cannot stage settings");
            File[] existing = live.listFiles();
            if (existing != null)
                for (File file : existing)
                    if (file.isFile()) {
                        try (FileInputStream in = new FileInputStream(file);
                                FileOutputStream out =
                                        new FileOutputStream(new File(prepared, file.getName()))) {
                            byte[] buffer = new byte[8192];
                            int n;
                            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                        }
                    }
            copyAllowed(approved, prepared, true);
            if (live.exists() && !live.renameTo(previous))
                throw new IOException("Cannot preserve settings");
            if (!prepared.renameTo(live)) {
                if (previous.exists()) previous.renameTo(live);
                throw new IOException("Cannot apply settings");
            }
            SafeZip.delete(previous);
        } finally {
            SafeZip.delete(prepared);
        }
    }

    public static void sanitizeTree(File root) throws IOException {
        File[] files = root.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                sanitizeTree(file);
            } else if (file.getParentFile().getName().equals("app_prefs")
                    && allowed(file.getName())) {
                String value =
                        sanitize(file.getName(), new String(read(file), StandardCharsets.UTF_8));
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(value.getBytes(StandardCharsets.UTF_8));
                }
            } else if (!file.getName().equals("settings-backup-version")) {
                if (!file.delete()) throw new IOException("Cannot discard unsafe settings");
            }
        }
    }
}
