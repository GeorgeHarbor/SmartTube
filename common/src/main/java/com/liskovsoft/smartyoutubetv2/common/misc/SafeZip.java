package com.liskovsoft.smartyoutubetv2.common.misc;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Extraction only into a new private staging directory. Callers never extract directly into app
 * data.
 */
public final class SafeZip {
    private SafeZip() {}

    public static void extract(File archive, File staging) throws IOException {
        if (staging.exists()) throw new IOException("Staging directory must be new");
        if (!staging.mkdirs()) throw new IOException("Cannot create staging directory");
        long total = 0;
        int entries = 0;
        Set<String> seen = new HashSet<>();
        String root = staging.getCanonicalPath() + File.separator;
        try (ZipInputStream input =
                new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = input.getNextEntry()) != null) {
                if (++entries > 10000) throw new IOException("Too many ZIP entries");
                String name = entry.getName();
                if (name.startsWith("/")
                        || name.contains("\\")
                        || name.indexOf(':') >= 0
                        || name.indexOf(0) >= 0) throw new IOException("Unsafe ZIP path");
                for (String part : name.split("/"))
                    if (part.equals("..") || part.equals("."))
                        throw new IOException("Unsafe ZIP path");
                File output = new File(staging, name);
                String canonical = output.getCanonicalPath();
                if (!canonical.startsWith(root) || !seen.add(canonical))
                    throw new IOException("Duplicate or unsafe ZIP path");
                if (entry.isDirectory()) {
                    if (input.read() != -1) throw new IOException("Directory entry contains data");
                    if (!output.mkdirs() && !output.isDirectory())
                        throw new IOException("Invalid directory");
                } else {
                    if (!output.getParentFile().mkdirs() && !output.getParentFile().isDirectory())
                        throw new IOException("Invalid parent");
                    try (FileOutputStream out = new FileOutputStream(output)) {
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            total += count;
                            if (total > 100L * 1024 * 1024)
                                throw new IOException("ZIP expansion limit");
                            out.write(buffer, 0, count);
                        }
                    }
                }
                input.closeEntry();
            }
        } catch (IOException failure) {
            delete(staging);
            throw failure;
        }
    }

    public static void delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) for (File child : files) delete(child);
        }
        file.delete();
    }
}
