package com.liskovsoft.smartyoutubetv2.common.misc;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Byte-oriented, bounded HTTP/1.x subset: no chunking, pipelining or ambiguous framing. */
final class DialRequest {
    String method, path, body;
    final Map<String, String> headers = new HashMap<>();

    static DialRequest read(InputStream input) throws IOException {
        DialRequest request = new DialRequest();
        String[] first = line(input, 2048).split(" ");
        if (first.length != 3 || !first[2].matches("HTTP/1\\.[01]"))
            throw new IOException("Invalid request line");
        request.method = first[0];
        request.path = first[1];
        int total = 0;
        while (true) {
            String line = line(input, 8192 - total);
            total += line.length() + 2;
            if (total > 8192) throw new IOException("Headers too large");
            if (line.isEmpty()) break;
            int colon = line.indexOf(':');
            if (colon < 1) throw new IOException("Invalid header");
            String key = line.substring(0, colon).toLowerCase(Locale.ROOT);
            if (request.headers.put(key, line.substring(colon + 1).trim()) != null)
                throw new IOException("Duplicate header");
        }
        if (request.headers.containsKey("transfer-encoding"))
            throw new IOException("Unsupported framing");
        int length = 0;
        try {
            length =
                    Integer.parseInt(
                            request.headers.containsKey("content-length")
                                    ? request.headers.get("content-length")
                                    : "0");
        } catch (NumberFormatException e) {
            throw new IOException("Invalid length");
        }
        if (length < 0 || length > 16384) throw new IOException("Body too large");
        byte[] body = new byte[length];
        int offset = 0;
        while (offset < length) {
            int n = input.read(body, offset, length - offset);
            if (n < 0) throw new EOFException();
            offset += n;
        }
        request.body = new String(body, StandardCharsets.UTF_8);
        return request;
    }

    private static String line(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int previous = -1;
        while (true) {
            int next = input.read();
            if (next < 0) throw new EOFException();
            if (next == 10) {
                if (previous != 13) throw new IOException("Invalid line ending");
                byte[] bytes = out.toByteArray();
                return new String(bytes, 0, bytes.length - 1, StandardCharsets.US_ASCII);
            }
            if (out.size() >= limit) throw new IOException("Line too large");
            out.write(next);
            previous = next;
        }
    }
}
