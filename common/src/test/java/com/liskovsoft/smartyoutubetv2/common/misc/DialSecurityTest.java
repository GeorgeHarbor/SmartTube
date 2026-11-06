package com.liskovsoft.smartyoutubetv2.common.misc;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class DialSecurityTest {
    private DialRequest parse(String request) throws IOException {
        return DialRequest.read(
                new ByteArrayInputStream(request.getBytes(StandardCharsets.US_ASCII)));
    }

    private void rejects(String value) throws Exception {
        try {
            parse(value);
            fail("Accepted malformed request");
        } catch (IOException expected) {
        }
    }

    @Test
    public void validRequestRetainsExactPayload() throws Exception {
        DialRequest r =
                parse(
                        "POST /apps/YouTube HTTP/1.1\r\nContent-Length: 3\r\n\r\na=b"
                                .replace("\\r", "\r")
                                .replace("\\n", "\n"));
        assertEquals("a=b", r.body);
    }

    @Test
    public void rejectsAmbiguousOrOversizedFraming() throws Exception {
        rejects("POST / HTTP/1.1\r\nContent-Length: -1\r\n\r\n");
        rejects("POST / HTTP/1.1\r\nContent-Length: 20000\r\n\r\n");
        rejects("POST / HTTP/1.1\r\nContent-Length: 0\r\nContent-Length: 0\r\n\r\n");
        rejects("POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n");
    }

    @Test
    public void boundsPerPeerAndGlobalAttempts() {
        DialRateLimit limiter = new DialRateLimit();
        for (int i = 0; i < 5; i++) assertTrue(limiter.allow("one", 0));
        assertFalse(limiter.allow("one", 0));
        for (int i = 0; i < 15; i++) assertTrue(limiter.allow("peer" + i, 0));
        assertFalse(limiter.allow("new", 0));
        assertTrue(limiter.allow("one", 60001));
    }
}
