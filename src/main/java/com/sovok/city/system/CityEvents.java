package com.sovok.city.system;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.sovok.city.Config;
import com.sovok.city.SovokCity;
import com.sovok.city.item.ModItems;
import com.sovok.city.world.Apartments;
import com.sovok.city.world.CityBuilder;
import com.sovok.city.world.CityData;
import com.sovok.city.world.DayEvent;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Вход в игру, квартиры, короткие сутки, события дня, пропаганда, команды. */
@EventBusSubscriber(modid = SovokCity.MODID)
public class CityEvents {
    private static double extraAcc = 0;
    private static long lastDt = -1;

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent e) {
        extraAcc = 0;
        lastDt = -1;
    }

    // ---------- вход игрока ----------
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ServerLevel ow = p.server.overworld();
        CityBuilder.ensureBuilt(ow);
        CityData d = CityData.get(ow);
        if (d.eventDay >= 0) announce(p, d.eventDay, d.current());
        boolean first = !PlayerStats.inCity(p);
        boolean hadFlat = d.owners.containsKey(p.getUUID());
        if (first) {
            PlayerStats.setWelcomed(p);
            Econ.give(p, new ItemStack(ModItems.COUPON.get(), 3));
            Econ.giveRubles(p, 20);
            Econ.give(p, new ItemStack(ModItems.MUG.get()));
            Econ.give(p, new ItemStack(ModItems.BLACK_BREAD.get(), 2));
            Econ.chat(p, "msg.sovokcity.welcome");
            Econ.chat(p, "msg.sovokcity.places", fmt(d.ration), fmt(d.casino), fmt(d.station));
        }
        if (!hadFlat) {
            int idx = Apartments.assign(p);
            if (idx >= 0) {
                BlockPos sp = Apartments.spawn(d, idx);
                p.setRespawnPosition(Level.OVERWORLD, sp, Apartments.yaw(idx), true, false);
                Econ.chat(p, "msg.sovokcity.apartment", idx + 1, fmt(sp));
                if (first) p.teleportTo(ow, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, Apartments.yaw(idx), 0f);
            } else {
                Econ.chat(p, "msg.sovokcity.no_apartments");
                if (first) p.teleportTo(ow, d.plaza.getX() + 0.5, d.plaza.getY(), d.plaza.getZ() + 0.5, 0f, 0f);
            }
        }
    }

    private static String fmt(BlockPos b) { return b.getX() + " " + b.getY() + " " + b.getZ(); }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (!e.isWasDeath()) return;
        restoreAfterDeath(e.getOriginal(), e.getEntity());
    }

    private static void restoreAfterDeath(Player from, Player to) {
        if (PlayerStats.inCity(from)) PlayerStats.setWelcomed(to);
        PlayerStats.setLastCouponDay(to, PlayerStats.lastCouponDay(from));
        PlayerStats.setJailUntil(to, PlayerStats.jailUntil(from));
        PlayerStats.setRating(to, PlayerStats.rating(from));
        PlayerStats.setThirst(to, 100);
        PlayerStats.setBody(to, 36.6);
        PlayerStats.setColdSecs(to, 0);
        PlayerStats.setDysSecs(to, 0);
        PlayerStats.setChill(to, 0);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.isEndConquered()) return;
        if (p.getRespawnPosition() != null) return; // квартира/кровать — уже респавн там
        ServerLevel ow = p.server.overworld();
        CityData d = CityData.get(ow);
        if (!d.built) return;
        p.teleportTo(ow, d.plaza.getX() + 0.5, d.plaza.getY(), d.plaza.getZ() + 0.5, 0f, 0f);
    }

    // ---------- время, события дня, пропаганда ----------
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        var server = e.getServer();
        ServerLevel ow = server.overworld();
        int tick = server.getTickCount();

        // сутки короче: подкручиваем время добавочными тиками
        if (ow.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            double per = 24000.0 / (Config.DAY_SECONDS.get() * 20.0) - 1.0;
            if (per > 0) {
                extraAcc += per;
                long add = (long) extraAcc;
                if (add > 0) {
                    extraAcc -= add;
                    ow.setDayTime(ow.getDayTime() + add);
                }
            }
            if (tick % 5 == 0) {
                server.getPlayerList().broadcastAll(
                        new ClientboundSetTimePacket(ow.getGameTime(), ow.getDayTime(), true), ow.dimension());
            }
        }

        CityData d = CityData.get(ow);
        if (!d.built) return;
        long dayTime = ow.getDayTime();
        long dt = dayTime % 24000L;

        // событие нового дня
        long day = dayTime / 24000L;
        if (day != d.eventDay) rollDay(ow, d, day);

        // комендантский час
        String key = null;
        if (lastDt >= 0) {
            if (crossed(lastDt, dt, 12000)) key = "msg.sovokcity.curfew_soon";
            else if (crossed(lastDt, dt, 13000)) key = "msg.sovokcity.curfew_now";
            else if (crossed(lastDt, dt, 23000)) key = "msg.sovokcity.curfew_end";
        }
        lastDt = dt;
        if (key == null && tick % 1800 == 0) key = "msg.sovokcity.prop." + (1 + ow.random.nextInt(6));
        if (key == null) return;
        for (ServerPlayer p : ow.players()) {
            if (d.inCity(p.position())) p.sendSystemMessage(Component.translatable(key));
        }
    }

    private static boolean crossed(long a, long b, long t) {
        if (b >= a) return a < t && b >= t;
        return t > a || t <= b; // перешагнули через полночь
    }

    private static void rollDay(ServerLevel ow, CityData d, long day) {
        DayEvent ev = DayEvent.roll(ow.random);
        d.event = ev.name();
        d.eventDay = day;
        d.setDirty();
        for (ServerPlayer p : ow.players()) {
            PlayerStats.addRating(p, ev == DayEvent.HOLIDAY ? 5 : 0);
            if (d.inCity(p.position())) PlayerStats.addRating(p, 2); // образцовый гражданин
            announce(p, day, ev);
            switch (ev) {
                case FOOD -> Econ.give(p, new ItemStack(ModItems.COUPON.get(), 2));
                case HOLIDAY -> Econ.giveRubles(p, 10);
                case FLU -> {
                    if (p.getRandom().nextBoolean() && PlayerStats.coldSecs(p) == 0) {
                        PlayerStats.setColdSecs(p, 420);
                        Econ.chat(p, "msg.sovokcity.got_cold");
                    }
                }
                default -> { }
            }
        }
    }

    public static void announce(ServerPlayer p, long day, DayEvent ev) {
        p.sendSystemMessage(Component.translatable("msg.sovokcity.day_header", day + 1,
                Component.translatable("event.sovokcity." + ev.key() + ".name")));
        p.sendSystemMessage(Component.translatable("event.sovokcity." + ev.key() + ".desc"));
    }

    // ---------- команды ----------
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("sovok")
                .then(Commands.literal("info").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CityData d = CityData.get(p.serverLevel());
                    if (!d.built) return 0;
                    Econ.chat(p, "msg.sovokcity.places", fmt(d.ration), fmt(d.casino), fmt(d.station));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("today").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CityData d = CityData.get(p.serverLevel());
                    announce(p, d.eventDay, d.current());
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("home").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ServerLevel ow = p.server.overworld();
                    CityData d = CityData.get(ow);
                    int idx = Apartments.assign(p);
                    if (idx < 0) { Econ.chat(p, "msg.sovokcity.no_apartments"); return 0; }
                    if (PlayerStats.jailUntil(p) > ow.getGameTime()) return 0;
                    BlockPos sp = Apartments.spawn(d, idx);
                    p.teleportTo(ow, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, Apartments.yaw(idx), 0f);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("rating").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Econ.chat(p, "msg.sovokcity.rating", PlayerStats.rating(p));
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.literal("set").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            PlayerStats.setRating(p, IntegerArgumentType.getInteger(ctx, "value"));
                            Econ.chat(p, "msg.sovokcity.rating", PlayerStats.rating(p));
                            return Command.SINGLE_SUCCESS;
                        }))))
                .then(Commands.literal("pardon").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    PlayerStats.setJailUntil(p, 1);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("hideout").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CityData d = CityData.get(p.serverLevel());
                    if (!d.built) return 0;
                    p.teleportTo(p.serverLevel(), d.hideout.getX() + 0.5, d.hideout.getY(), d.hideout.getZ() + 0.5, 0f, 0f);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
