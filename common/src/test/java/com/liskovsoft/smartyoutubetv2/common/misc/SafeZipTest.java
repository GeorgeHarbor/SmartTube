package com.liskovsoft.smartyoutubetv2.common.misc;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.*;
import java.util.zip.*;

public class SafeZipTest {
    @Test
    public void rejectsTraversalAndCleansStaging() throws Exception {
        File base = java.nio.file.Files.createTempDirectory("safezip").toFile(),
                zip = new File(base, "test.zip"),
                stage = new File(base, "staging");
        try {
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
                out.putNextEntry(new ZipEntry("../outside"));
                out.write(1);
                out.closeEntry();
            }
            try {
                SafeZip.extract(zip, stage);
                fail("Traversal accepted");
            } catch (IOException expected) {
            }
            assertFalse(stage.exists());
            assertFalse(new File(base, "outside").exists());
        } finally {
            SafeZip.delete(base);
        }
    }

    @Test
    public void extractsOnlyIntoNewStagingDirectory() throws Exception {
        File base = java.nio.file.Files.createTempDirectory("safezip").toFile(),
                zip = new File(base, "test.zip"),
                stage = new File(base, "staging");
        try {
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
                out.putNextEntry(new ZipEntry("files/app_prefs/player_data"));
                out.write("settings".getBytes("UTF-8"));
                out.closeEntry();
            }
            SafeZip.extract(zip, stage);
            assertTrue(new File(stage, "files/app_prefs/player_data").isFile());
            try {
                SafeZip.extract(zip, stage);
                fail("Existing target accepted");
            } catch (IOException expected) {
            }
        } finally {
            SafeZip.delete(base);
        }
    }
    @Test public void rejectsNormalizedDuplicatePaths()throws Exception{
        File base=java.nio.file.Files.createTempDirectory("duplicate-zip").toFile(),zip=new File(base,"test.zip"),stage=new File(base,"stage");
        try{
            try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))){
                for(String name:new String[]{"nested//value","nested/value"}){out.putNextEntry(new ZipEntry(name));out.write(1);out.closeEntry();}
            }
            try{SafeZip.extract(zip,stage);fail("Normalized duplicate accepted");}catch(IOException expected){}
            assertFalse(stage.exists());
        }finally{SafeZip.delete(base);}
    }
    @Test public void rejectsCompressedExpansionBeyondLimit()throws Exception{
        File base=java.nio.file.Files.createTempDirectory("expansion-zip").toFile(),zip=new File(base,"test.zip"),stage=new File(base,"stage");
        try{
            try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))){out.putNextEntry(new ZipEntry("large"));byte[] zeros=new byte[1024*1024];for(int n=0;n<101;n++)out.write(zeros);out.closeEntry();}
            try{SafeZip.extract(zip,stage);fail("Expansion limit ignored");}catch(IOException expected){}
            assertFalse(stage.exists());
        }finally{SafeZip.delete(base);}
    }
    @Test public void rejectsDirectoryPayloadInsteadOfDrainingIt()throws Exception{
        File base=java.nio.file.Files.createTempDirectory("directory-zip").toFile(),zip=new File(base,"test.zip"),stage=new File(base,"stage");
        try{
            try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))){out.putNextEntry(new ZipEntry("directory/"));out.write(1);out.closeEntry();}
            try{SafeZip.extract(zip,stage);fail("Directory payload accepted");}catch(IOException expected){}
            assertFalse(stage.exists());
        }finally{SafeZip.delete(base);}
    }
}
