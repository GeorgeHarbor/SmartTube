package com.liskovsoft.smartyoutubetv2.common.misc;

import java.util.*;

final class DialRateLimit {
    private final Map<String, ArrayDeque<Long>> peers = new HashMap<>();
    private final ArrayDeque<Long> global = new ArrayDeque<>();

    synchronized boolean allow(String address, long now) {
        while (!global.isEmpty() && now - global.peekFirst() >= 60000) global.removeFirst();
        java.util.Iterator<Map.Entry<String, ArrayDeque<Long>>> iterator =
                peers.entrySet().iterator();
        while (iterator.hasNext()) {
            ArrayDeque<Long> events = iterator.next().getValue();
            while (!events.isEmpty() && now - events.peekFirst() >= 60000) events.removeFirst();
            if (events.isEmpty()) iterator.remove();
        }
        if (global.size() >= 20) return false;
        ArrayDeque<Long> peer = peers.get(address);
        if (peer == null) {
            peer = new ArrayDeque<>();
            peers.put(address, peer);
        }
        if (peer.size() >= 5) return false;
        peer.addLast(now);
        global.addLast(now);
        return true;
    }
}
