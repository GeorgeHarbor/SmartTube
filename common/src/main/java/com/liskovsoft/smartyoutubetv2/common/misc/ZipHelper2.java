package com.liskovsoft.smartyoutubetv2.common.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipHelper2 {
    public static void zipDirectory(File sourceDir, File zipFile) {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
            zipFileRecursive(zos, sourceDir, sourceDir.getName() + "/");
            zos.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void zipFileRecursive(ZipOutputStream zos, File file, String base) throws Exception {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    zipFileRecursive(zos, child, base + child.getName() + "/");
                }
            }
        } else {
            FileInputStream fis = new FileInputStream(file);
            zos.putNextEntry(new ZipEntry(base.substring(0, base.length() -1))); // strip "/" at the end to mark as file
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) zos.write(buf, 0, len);
            fis.close();
            zos.closeEntry();
        }
    }

    public static void unzip(File zipFile, File targetRoot) {
        File staging=new File(targetRoot.getParentFile(),".import-"+java.util.UUID.randomUUID());
        try {
            SafeZip.extract(zipFile,staging);
            SettingsBackup.sanitizeTree(staging);
            com.liskovsoft.sharedutils.helpers.FileHelpers.copy(staging,targetRoot);
        } catch(IOException failure) { throw new IllegalStateException("Invalid settings archive",failure); }
        finally {SafeZip.delete(staging);}
    }

    public static boolean hasRootDir(File zipFile, String rootDir) {
        boolean result = false;

        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry entry;

            String prefix = rootDir + "/";

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().startsWith(prefix)) {
                    zis.closeEntry();
                    result = true;
                    break;
                }
                zis.closeEntry();
            }
            zis.close();
        } catch (Exception e) { e.printStackTrace(); }

        return result;
    }
}
