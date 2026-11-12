package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Minimal adb client that runs a shell command on this device through its own network debugging (localhost:5555).<br/>
 * Lets the app do what only the shell user may, e.g. disable another app. The user approves the key once.
 */
public class AdbShell {
    private static final int CNXN = 0x4e584e43;
    private static final int AUTH = 0x48545541;
    private static final int OPEN = 0x4e45504f;
    private static final int OKAY = 0x59414b4f;
    private static final int CLSE = 0x45534c43;
    private static final int WRTE = 0x45545257;
    private static final int AUTH_TOKEN = 1;
    private static final int AUTH_SIGNATURE = 2;
    private static final int AUTH_RSAPUBLICKEY = 3;
    private static final int PORT = 5555;
    // Long enough for the user to answer the "Allow debugging?" prompt
    private static final int TIMEOUT_MS = 60_000;
    private static final byte[] SHA1_DIGEST_INFO = {
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14
    };

    /**
     * Blocking. Returns the command output.
     */
    public static String run(Context context, String command) throws IOException, GeneralSecurityException {
        KeyPair keys = getKeys(context);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", PORT), 3_000);
            socket.setSoTimeout(TIMEOUT_MS);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            OutputStream out = socket.getOutputStream();

            write(out, CNXN, 0x01000000, 4096, "host::\0".getBytes(StandardCharsets.UTF_8));

            boolean signed = false;
            Message msg = read(in);
            while (msg.command == AUTH) {
                if (!signed) {
                    write(out, AUTH, AUTH_SIGNATURE, 0, sign(keys, msg.data));
                    signed = true;
                } else { // unknown key: ask the user to accept it
                    write(out, AUTH, AUTH_RSAPUBLICKEY, 0, encodePublicKey((RSAPublicKey) keys.getPublic()));
                }
                msg = read(in);
            }
            if (msg.command != CNXN) {
                throw new IOException("Unexpected adb reply: " + Integer.toHexString(msg.command));
            }

            int localId = 1;
            write(out, OPEN, localId, 0, ("shell:" + command + "\0").getBytes(StandardCharsets.UTF_8));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while (true) {
                msg = read(in);
                if (msg.command == WRTE) {
                    output.write(msg.data);
                    write(out, OKAY, localId, msg.arg0, new byte[0]);
                } else if (msg.command == CLSE) {
                    return output.toString("UTF-8");
                }
            }
        }
    }

    private static KeyPair getKeys(Context context) throws IOException, GeneralSecurityException {
        File privateFile = new File(context.getFilesDir(), "adbkey");
        File publicFile = new File(context.getFilesDir(), "adbkey.pub");
        KeyFactory factory = KeyFactory.getInstance("RSA");

        if (privateFile.exists() && publicFile.exists()) {
            return new KeyPair(
                    factory.generatePublic(new X509EncodedKeySpec(readFile(publicFile))),
                    factory.generatePrivate(new PKCS8EncodedKeySpec(readFile(privateFile))));
        }

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keys = generator.generateKeyPair();
        writeFile(privateFile, keys.getPrivate().getEncoded());
        writeFile(publicFile, keys.getPublic().getEncoded());
        return keys;
    }

    /**
     * adbd verifies the token as an RSA PKCS#1 signature of a SHA-1 digest
     */
    private static byte[] sign(KeyPair keys, byte[] token) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("NONEwithRSA");
        signature.initSign(keys.getPrivate());
        signature.update(SHA1_DIGEST_INFO);
        signature.update(token);
        return signature.sign();
    }

    /**
     * Android's adb public key format: base64 of {len, n0inv, n, rr, e} (little-endian), then " name\0"
     */
    static byte[] encodePublicKey(RSAPublicKey key) {
        int words = 2048 / 32;
        BigInteger n = key.getModulus();
        BigInteger r32 = BigInteger.ONE.shiftLeft(32);
        BigInteger n0inv = r32.subtract(n.mod(r32).modInverse(r32));
        BigInteger rr = BigInteger.ONE.shiftLeft(4096).mod(n);

        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + words * 4 * 2 + 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(words);
        buffer.putInt(n0inv.intValue());
        buffer.put(toLittleEndian(n, words * 4));
        buffer.put(toLittleEndian(rr, words * 4));
        buffer.putInt(key.getPublicExponent().intValue());

        String encoded = Base64.encodeToString(buffer.array(), Base64.NO_WRAP) + " SmartTube@tv\0";
        return encoded.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] toLittleEndian(BigInteger value, int size) {
        byte[] bigEndian = value.toByteArray(); // may have a leading sign byte
        byte[] result = new byte[size];
        for (int i = 0; i < size && i < bigEndian.length; i++) {
            result[i] = bigEndian[bigEndian.length - 1 - i];
        }
        return result;
    }

    private static void write(OutputStream out, int command, int arg0, int arg1, byte[] data) throws IOException {
        int checksum = 0;
        for (byte b : data) {
            checksum += b & 0xff;
        }

        ByteBuffer header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(command).putInt(arg0).putInt(arg1).putInt(data.length).putInt(checksum).putInt(~command);
        out.write(header.array());
        out.write(data);
        out.flush();
    }

    private static Message read(DataInputStream in) throws IOException {
        byte[] header = new byte[24];
        in.readFully(header);
        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        Message msg = new Message();
        msg.command = buffer.getInt();
        msg.arg0 = buffer.getInt();
        buffer.getInt(); // arg1
        msg.data = new byte[buffer.getInt()];
        in.readFully(msg.data);
        return msg;
    }

    private static byte[] readFile(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            in.readFully(data);
        }
        return data;
    }

    private static void writeFile(File file, byte[] data) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(data);
        }
    }

    private static class Message {
        int command;
        int arg0;
        byte[] data;
    }
}
