package com.sovok.city.system;

import com.sovok.city.SovokCity;
import com.sovok.city.net.Net;
import com.sovok.city.net.StatsPayload;
import com.sovok.city.world.CityData;
import com.sovok.city.world.DayEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Жажда, температура тела и болезни (простуда, дизентерия). Логика раз в секунду. */
@EventBusSubscriber(modid = SovokCity.MODID)
public class Survival {

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.tickCount % 20 != 0) return;
        ServerLevel level = p.serverLevel();

        if (!p.isCreative() && !p.isSpectator() && p.isAlive()) {
            tickThirst(p);
            tickTemperature(p, level);
            tickDiseases(p, level);
        }
        sync(p, level);
    }

    // ---------- Жажда ----------
    private static void tickThirst(ServerPlayer p) {
        double drain = 0.04;
        if (p.isSprinting()) drain += 0.08;
        if (PlayerStats.body(p) > 37.5) drain += 0.06;
        if (PlayerStats.dysSecs(p) > 0) drain += 0.08;
        if (CityData.get(p.serverLevel()).current() == DayEvent.HEAT) drain *= 2;
        PlayerStats.addThirst(p, -drain);
        double t = PlayerStats.thirst(p);
        if (t <= 25) eff(p, MobEffects.MOVEMENT_SLOWDOWN, 0);
        if (t <= 10) { eff(p, MobEffects.WEAKNESS, 0); eff(p, MobEffects.DIG_SLOWDOWN, 0); }
        if (t <= 0) {
            if (p.tickCount % 80 == 0) p.hurt(p.level().damageSources().generic(), 1.0F);
            Econ.msg(p, "msg.sovokcity.dehydrated");
        } else if (t <= 15 && p.tickCount % 200 == 0) {
            Econ.msg(p, "msg.sovokcity.thirsty");
        }
    }

    // ---------- Температура ----------
    private static void tickTemperature(ServerPlayer p, ServerLevel level) {
        BlockPos pos = p.blockPosition();
        double amb = 5 + level.getBiome(pos).value().getBaseTemperature() * 20.0;
        long dt = level.getDayTime() % 24000L;
        boolean night = dt >= 13000 && dt < 23000;
        boolean sky = level.canSeeSky(pos.above());
        DayEvent ev = CityData.get(level).current();

        if (level.dimension() == Level.NETHER) amb = 45;
        else if (level.dimension() == Level.END) amb = -5;
        else {
            if (night) amb -= 8;
            if (sky && level.isRaining()) amb -= 5;
            if (pos.getY() > 100) amb -= (pos.getY() - 100) * 0.05;
        }
        if (ev == DayEvent.HEAT && level.dimension() == Level.OVERWORLD) amb += 6;
        if (!sky) amb = amb * 0.4 + (ev == DayEvent.NO_HEATING ? 2 : 11); // в помещении теплее (если топят)
        if (p.isInWaterOrRain()) amb -= 6;
        if (nearHeat(level, pos)) amb += 15;

        int armor = 0;
        for (ItemStack s : p.getArmorSlots()) if (!s.isEmpty()) armor++;
        if (amb < 20) amb += 2.0 * armor;

        double target = 36.6 + Math.max(-12, Math.min(8, (amb - 21) * 0.12));
        if (p.isSprinting()) target += 0.4;
        double body = PlayerStats.body(p);
        body += (target - body) * 0.03;
        PlayerStats.setBody(p, body);

        if (body < 35.5) eff(p, MobEffects.MOVEMENT_SLOWDOWN, 0);
        if (body < 34.0) { eff(p, MobEffects.MOVEMENT_SLOWDOWN, 1); eff(p, MobEffects.DIG_SLOWDOWN, 0); }
        if (body < 32.5) p.hurt(level.damageSources().freeze(), 1.0F);
        if (body < 35.5 && p.tickCount % 300 == 0) Econ.msg(p, "msg.sovokcity.cold");

        if (body > 38.5) eff(p, MobEffects.WEAKNESS, 0);
        if (body > 40.0) p.hurt(level.damageSources().generic(), 1.0F);
        if (body > 38.0 && p.tickCount % 300 == 0) Econ.msg(p, "msg.sovokcity.hot");

        // простуда от переохлаждения
        int chill = PlayerStats.chill(p);
        chill = body < 35.2 ? chill + 1 : chill - 1;
        PlayerStats.setChill(p, chill);
        if (chill >= 60 && PlayerStats.coldSecs(p) == 0) {
            PlayerStats.setColdSecs(p, 420);
            PlayerStats.setChill(p, 0);
            Econ.chat(p, "msg.sovokcity.got_cold");
        }
    }

    private static boolean nearHeat(ServerLevel level, BlockPos pos) {
        for (BlockPos bp : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3))) {
            BlockState s = level.getBlockState(bp);
            if (s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.MAGMA_BLOCK)) return true;
            if (s.getFluidState().is(FluidTags.LAVA)) return true;
            if (s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT)
                    && (s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE) || s.is(Blocks.FURNACE)
                    || s.is(Blocks.BLAST_FURNACE) || s.is(Blocks.SMOKER))) return true;
        }
        return false;
    }

    // ---------- Болезни ----------
    private static void tickDiseases(ServerPlayer p, ServerLevel level) {
        int cold = PlayerStats.coldSecs(p);
        if (cold > 0) {
            PlayerStats.setColdSecs(p, cold - 1);
            eff(p, MobEffects.WEAKNESS, 0);
            if (level.random.nextInt(40) == 0) Econ.msg(p, "msg.sovokcity.cough");
            if (cold == 1) Econ.chat(p, "msg.sovokcity.cold_over");
        }
        int dys = PlayerStats.dysSecs(p);
        if (dys > 0) {
            PlayerStats.setDysSecs(p, dys - 1);
            eff(p, MobEffects.HUNGER, 1);
            p.getFoodData().addExhaustion(0.6F);
            if (level.random.nextInt(25) == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false, true));
            }
            if (dys == 1) Econ.chat(p, "msg.sovokcity.dys_over");
        }
    }

    public static void infectDysentery(ServerPlayer p, double chance) {
        if (PlayerStats.dysSecs(p) > 0) return;
        if (p.getRandom().nextDouble() < chance) {
            PlayerStats.setDysSecs(p, 300);
            Econ.chat(p, "msg.sovokcity.got_dys");
        }
    }

    public static void cureAll(ServerPlayer p) {
        PlayerStats.setColdSecs(p, 0);
        PlayerStats.setDysSecs(p, 0);
        PlayerStats.setChill(p, 0);
        p.removeEffect(MobEffects.HUNGER);
        p.removeEffect(MobEffects.WEAKNESS);
        p.removeEffect(MobEffects.CONFUSION);
        Econ.msg(p, "msg.sovokcity.cured");
    }

    // ---------- Еда и питьё ванильных предметов ----------
    @SubscribeEvent
    public static void onFinishUse(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack s = e.getItem();
        if (s.is(Items.ROTTEN_FLESH) || s.is(Items.CHICKEN) || s.is(Items.PORKCHOP) || s.is(Items.BEEF)
                || s.is(Items.MUTTON) || s.is(Items.RABBIT) || s.is(Items.COD) || s.is(Items.SALMON)
                || s.is(Items.SPIDER_EYE)) {
            infectDysentery(p, 0.35);
        }
        if (s.is(Items.POTION)) {
            PotionContents pc = s.get(DataComponents.POTION_CONTENTS);
            if (pc != null && pc.is(Potions.WATER)) PlayerStats.addThirst(p, 25);
        } else if (s.is(Items.MILK_BUCKET)) PlayerStats.addThirst(p, 10);
        else if (s.is(Items.MELON_SLICE)) PlayerStats.addThirst(p, 6);
        else if (s.is(Items.APPLE) || s.is(Items.SWEET_BERRIES)) PlayerStats.addThirst(p, 4);
    }

    private static void eff(ServerPlayer p, Holder<MobEffect> effect, int amp) {
        p.addEffect(new MobEffectInstance(effect, 80, amp, true, false, true));
    }

    private static void sync(ServerPlayer p, ServerLevel level) {
        long now = level.getGameTime();
        int jail = (int) Math.max(0, (PlayerStats.jailUntil(p) - now) / 20);
        int alert = (now - PlayerStats.lastCrime(p) < 200) ? 1 : 0;
        Net.send(p, new StatsPayload((int) Math.round(PlayerStats.thirst(p)),
                (int) Math.round(PlayerStats.body(p) * 10), PlayerStats.coldSecs(p),
                PlayerStats.dysSecs(p), jail, alert, PlayerStats.rating(p),
                CityData.get(level).current().ordinal()));
    }
}
