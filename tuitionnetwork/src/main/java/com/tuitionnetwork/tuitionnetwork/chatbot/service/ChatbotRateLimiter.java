package com.tuitionnetwork.tuitionnetwork.chatbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatbotRateLimiter {

    private final int maxPerMinute;
    private final ConcurrentHashMap<UUID, Window> windows = new ConcurrentHashMap<>();

    public ChatbotRateLimiter(@Value("${chatbot.rate-limit.per-minute:30}") int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public boolean allow(UUID accountId) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(accountId, ignored -> new Window(now));
        synchronized (window) {
            if (now - window.startedAt >= 60_000L) {
                window.startedAt = now;
                window.count = 0;
            }
            if (window.count >= maxPerMinute) {
                return false;
            }
            window.count++;
            return true;
        }
    }

    private static final class Window {
        private long startedAt;
        private int count;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
