package com.slop.pof.util;

import java.util.concurrent.ThreadLocalRandom;

/** Parses config amounts such as {@code 1}, {@code 4-12}, and {@code 1..2}. */
public final class Amounts {
    private Amounts() {
    }

    public static int roll(String raw, ThreadLocalRandom random) {
        int[] range = parse(raw);
        if (range[0] >= range[1]) {
            return range[0];
        }
        return range[0] + random.nextInt(range[1] - range[0] + 1);
    }

    public static int[] parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new int[]{1, 1};
        }
        String text = raw.trim();
        String[] parts = text.contains("..") ? text.split("\\.\\.", 2) : text.split("-", 2);
        int low = positive(parts[0]);
        int high = parts.length > 1 ? positive(parts[1]) : low;
        if (low > high) {
            int swap = low;
            low = high;
            high = swap;
        }
        return new int[]{low, high};
    }

    private static int positive(String token) {
        try {
            return Math.max(1, Integer.parseInt(token.trim()));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }
}
