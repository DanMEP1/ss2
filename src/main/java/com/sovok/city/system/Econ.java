package com.sovok.city.system;

import com.sovok.city.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public final class Econ {
    private Econ() {}

    public static int count(Player p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    public static boolean take(Player p, Item item, int amount) {
        if (count(p, item) < amount) return false;
        int left = amount;
        for (ItemStack s : p.getInventory().items) {
            if (left <= 0) break;
            if (s.is(item)) {
                int d = Math.min(left, s.getCount());
                s.shrink(d);
                left -= d;
            }
        }
        return true;
    }

    public static void give(Player p, ItemStack st) {
        if (st.isEmpty()) return;
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    public static void giveRubles(Player p, int n) {
        while (n > 0) {
            int c = Math.min(n, 64);
            give(p, new ItemStack(ModItems.RUBLE.get(), c));
            n -= c;
        }
    }

    /** Цена с учётом соц. рейтинга: при низком рейтинге всё дороже в 1.5 раза. */
    public static int scale(Player p, int base) {
        return PlayerStats.rating(p) <= com.sovok.city.Config.LOW_RATING.get() ? (int) Math.ceil(base * 1.5) : base;
    }

    public static void msg(Player p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args), true);
    }

    public static void chat(Player p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable(key, args));
    }

    /** Найти предмет Upgrader из стороннего мода по ID из конфига. */
    public static Optional<Item> upgraderItem() {
        ResourceLocation rl = ResourceLocation.tryParse(com.sovok.city.Config.UPGRADER_ID.get());
        if (rl == null) return Optional.empty();
        return BuiltInRegistries.ITEM.getOptional(rl);
    }

    public static boolean isUpgrader(ItemStack st) {
        if (st.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(st.getItem());
        if (key.toString().equals(com.sovok.city.Config.UPGRADER_ID.get())) return true;
        return !key.getNamespace().equals("minecraft") && key.getNamespace().contains("upgrader");
    }
}
