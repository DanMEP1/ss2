package com.sovok.city.system;

import com.sovok.city.Config;
import com.sovok.city.SovokCity;
import com.sovok.city.world.CityData;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Преступления и наказания: игра с Upgrader в подпольном казино, нарушение комендантского часа.
 * Игрока арестовывают только если милиционер его ВИДИТ (см. ArrestGoal).
 */
@EventBusSubscriber(modid = SovokCity.MODID)
public class Crime {
    /** Сколько тиков после нелегального действия игрок «в розыске» (виден = арест). */
    public static final int WANTED_TICKS = 200;

    public static boolean isNight(ServerLevel level) {
        long dt = level.getDayTime() % 24000L;
        return dt >= 13000 && dt < 23000;
    }

    /** Причина для ареста сейчас (casino / curfew) либо null. */
    public static String reason(ServerPlayer p) {
        if (p.level().dimension() != Level.OVERWORLD) return null;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (PlayerStats.jailUntil(p) > now) return null;
        CityData d = CityData.get(level);
        if (!d.built) return null;
        if (now - PlayerStats.lastCrime(p) < WANTED_TICKS) return "casino";
        if (Config.CURFEW.get() && isNight(level) && d.inCity(p.position())
                && level.canSeeSky(p.blockPosition().above())) return "curfew";
        return null;
    }

    public static void markCrime(ServerPlayer p) {
        PlayerStats.setLastCrime(p, p.serverLevel().getGameTime());
    }

    public static void arrest(ServerPlayer p, String reason) {
        ServerLevel level = p.serverLevel();
        CityData d = CityData.get(level);
        if (!d.built) return;
        boolean casino = "casino".equals(reason);
        boolean assault = "assault".equals(reason);
        int secs = assault ? Config.JAIL_SECONDS.get() * 3 / 2
                : casino ? Config.JAIL_SECONDS.get() : Math.max(10, Config.JAIL_SECONDS.get() / 2);
        int drop = assault ? 15 : casino ? 10 : 5;
        PlayerStats.addRating(p, -drop);

        // конфискация
        int seized = 0;
        for (ItemStack s : p.getInventory().items) {
            if (Econ.isUpgrader(s)) { seized += s.getCount(); s.setCount(0); }
        }
        int rubles = Econ.count(p, com.sovok.city.item.ModItems.RUBLE.get());
        int fine = assault ? rubles / 3 : casino ? rubles / 2 : Math.min(5, rubles);
        Econ.take(p, com.sovok.city.item.ModItems.RUBLE.get(), fine);

        PlayerStats.setJailUntil(p, level.getGameTime() + secs * 20L);
        PlayerStats.setLastCrime(p, -1_000_000);
        BlockPos j = d.jail;
        p.teleportTo(level, j.getX() + 0.5, j.getY(), j.getZ() + 0.5, 0f, 0f);

        Component announce = Component.translatable(assault ? "msg.sovokcity.arrest_assault"
                : casino ? "msg.sovokcity.arrest_casino" : "msg.sovokcity.arrest_curfew", p.getDisplayName());
        level.getServer().getPlayerList().broadcastSystemMessage(announce, false);
        Econ.chat(p, "msg.sovokcity.arrest_you", secs, fine, seized);
        Econ.chat(p, "msg.sovokcity.rating_drop", PlayerStats.rating(p), -drop);
    }

    // ---------- Обнаружение нарушений ----------
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer p)) return;
        if (Econ.isUpgrader(e.getItemStack()) && CityData.get(p.serverLevel()).inIllegal(p.position())) markCrime(p);
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent e) {
        var src = e.getParseResults().getContext().getSource();
        if (!(src.getEntity() instanceof ServerPlayer p)) return;
        String cmd = e.getParseResults().getReader().getString().toLowerCase();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        if (cmd.startsWith("upgrade") && CityData.get(p.serverLevel()).inIllegal(p.position())) markCrime(p);
    }

    /** Нападение на милиционера = мгновенный арест и минус к рейтингу. */
    @SubscribeEvent
    public static void onAttackMilitia(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof com.sovok.city.entity.Militiaman)) return;
        if (e.getSource().getEntity() instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator()
                && PlayerStats.jailUntil(p) <= p.serverLevel().getGameTime()) {
            e.setCanceled(true);
            arrest(p, "assault");
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getPlayer() instanceof ServerPlayer p && PlayerStats.jailUntil(p) > p.serverLevel().getGameTime()) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        CityData d = CityData.get(level);
        if (!d.built || level.dimension() != Level.OVERWORLD) return;

        // тюрьма
        long ju = PlayerStats.jailUntil(p);
        if (ju > 0) {
            if (now >= ju) {
                PlayerStats.setJailUntil(p, 0);
                BlockPos pl = d.plaza;
                p.teleportTo(level, pl.getX() + 0.5, pl.getY(), pl.getZ() + 0.5, 0f, 0f);
                Econ.chat(p, "msg.sovokcity.released");
            } else if (p.blockPosition().distSqr(d.jail) > 36) {
                p.teleportTo(level, d.jail.getX() + 0.5, d.jail.getY(), d.jail.getZ() + 0.5, 0f, 0f);
            }
            return;
        }

        // игра с Upgrader в подпольном казино
        if (!d.inIllegal(p.position())) { p.getPersistentData().putInt("sv_still", 0); return; }
        boolean holds = Econ.isUpgrader(p.getMainHandItem()) || Econ.isUpgrader(p.getOffhandItem());
        String menu = p.containerMenu.getClass().getName().toLowerCase();
        if (holds || (p.containerMenu != p.inventoryMenu && menu.contains("upgrad"))) markCrime(p);

        // GUI из чужого мода может быть клиентским: считаем «стоит с Upgrader в инвентаре» за игру
        boolean has = false;
        for (ItemStack s : p.getInventory().items) if (Econ.isUpgrader(s)) { has = true; break; }
        double sx = PlayerStats.getD(p, "sv_sx", 0), sz = PlayerStats.getD(p, "sv_sz", 0);
        int still = PlayerStats.getI(p, "sv_still", 0);
        if (has && Math.abs(p.getX() - sx) < 0.6 && Math.abs(p.getZ() - sz) < 0.6) still++; else still = 0;
        p.getPersistentData().putDouble("sv_sx", p.getX());
        p.getPersistentData().putDouble("sv_sz", p.getZ());
        p.getPersistentData().putInt("sv_still", still);
        if (still >= 6) markCrime(p);
    }
}
