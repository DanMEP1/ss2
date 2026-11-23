package com.sovok.city.system;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Хранение параметров реализма в persistent data игрока. */
public final class PlayerStats {
    private PlayerStats() {}

    private static CompoundTag t(Player p) { return p.getPersistentData(); }

    public static double getD(Player p, String k, double def) {
        CompoundTag t = t(p);
        return t.contains(k) ? t.getDouble(k) : def;
    }
    public static int getI(Player p, String k, int def) {
        CompoundTag t = t(p);
        return t.contains(k) ? t.getInt(k) : def;
    }
    public static long getL(Player p, String k, long def) {
        CompoundTag t = t(p);
        return t.contains(k) ? t.getLong(k) : def;
    }

    public static double thirst(Player p) { return getD(p, "sv_thirst", 100); }
    public static void setThirst(Player p, double v) { t(p).putDouble("sv_thirst", Math.max(0, Math.min(100, v))); }
    public static void addThirst(Player p, double d) { setThirst(p, thirst(p) + d); }

    public static double body(Player p) { return getD(p, "sv_body", 36.6); }
    public static void setBody(Player p, double v) { t(p).putDouble("sv_body", Math.max(25, Math.min(45, v))); }

    public static int coldSecs(Player p) { return getI(p, "sv_cold", 0); }
    public static void setColdSecs(Player p, int v) { t(p).putInt("sv_cold", Math.max(0, v)); }
    public static int dysSecs(Player p) { return getI(p, "sv_dys", 0); }
    public static void setDysSecs(Player p, int v) { t(p).putInt("sv_dys", Math.max(0, v)); }
    public static int chill(Player p) { return getI(p, "sv_chill", 0); }
    public static void setChill(Player p, int v) { t(p).putInt("sv_chill", Math.max(0, v)); }

    public static long lastCouponDay(Player p) { return getL(p, "sv_couponDay", -1); }
    public static void setLastCouponDay(Player p, long d) { t(p).putLong("sv_couponDay", d); }

    public static long jailUntil(Player p) { return getL(p, "sv_jailUntil", 0); }
    public static void setJailUntil(Player p, long g) { t(p).putLong("sv_jailUntil", g); }

    public static long lastCrime(Player p) { return getL(p, "sv_crime", -1_000_000); }
    public static void setLastCrime(Player p, long g) { t(p).putLong("sv_crime", g); }

    public static int order(Player p) { return getI(p, "sv_order", 0); }
    public static void setOrder(Player p, int v) { t(p).putInt("sv_order", v); }

    public static boolean inCity(Player p) { return t(p).getBoolean("sv_welcomed"); }
    public static void setWelcomed(Player p) { t(p).putBoolean("sv_welcomed", true); }

    public static int rating(Player p) { return getI(p, "sv_rating", 50); }
    public static void setRating(Player p, int v) { t(p).putInt("sv_rating", Math.max(0, Math.min(100, v))); }
    public static void addRating(Player p, int d) { setRating(p, rating(p) + d); }
}
