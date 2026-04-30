package com.deathmotion.totemguard.common.util;

import net.kyori.adventure.text.format.TextColor;

/**
 * TotemGuard · Forged Gold palette · v1.
 * Use TextColor constants for Adventure components,
 * HEX strings for MiniMessage / config-driven text.
 */
public final class Palette {
    // ---- Brand ----
    public static final TextColor GOLD_50 = TextColor.fromHexString("#FFF6D6");
    public static final TextColor GOLD_100 = TextColor.fromHexString("#FEE067");
    public static final TextColor GOLD_200 = TextColor.fromHexString("#FBAF00"); // primary
    public static final TextColor GOLD_300 = TextColor.fromHexString("#C68500");
    public static final TextColor GOLD_400 = TextColor.fromHexString("#8F7440"); // bronze sep
    // ---- Parchment ----
    public static final TextColor PARCH_50 = TextColor.fromHexString("#F4ECD8");
    public static final TextColor PARCH_100 = TextColor.fromHexString("#E7DEC4");
    public static final TextColor PARCH_200 = TextColor.fromHexString("#B8AC8F");
    public static final TextColor PARCH_300 = TextColor.fromHexString("#6E6651");
    // ---- Semantic ----
    public static final TextColor SUCCESS = TextColor.fromHexString("#6FB04A");
    public static final TextColor WARN = TextColor.fromHexString("#E68A2E");
    public static final TextColor DANGER = TextColor.fromHexString("#D4452C");
    public static final TextColor DANGER_SOFT = TextColor.fromHexString("#F5A48F");
    // note: "info" / debug values → reuse PARCH_200 (no separate INFO token).
    public static final TextColor VIOLET = TextColor.fromHexString("#8E6CB0");
    // ---- Semantic aliases (read at call sites for intent) ----
    public static final TextColor BRAND = GOLD_200;
    public static final TextColor VALUE = GOLD_100;     // player names, numbers (on neutral lines)
    public static final TextColor VALUE_ON_DANGER = DANGER_SOFT;  // variables inside red errors
    public static final TextColor LABEL = PARCH_100;    // "Player:", "Check:"
    public static final TextColor CONNECTIVE = PARCH_200;    // "failed", "VL[", debug, ms units
    public static final TextColor CAPTION = PARCH_300;    // "»", footers
    public static final TextColor SEPARATOR = GOLD_400;     // "|"

    private Palette() {
    }
}