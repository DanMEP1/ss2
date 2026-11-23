package com.sovok.city.entity;

import com.sovok.city.system.Crime;
import com.sovok.city.world.CityData;
import com.sovok.city.world.DayEvent;
import net.minecraft.server.level.ServerLevel;
import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Если милиционер ВИДИТ нарушителя — бежит к нему и арестовывает. */
public class ArrestGoal extends Goal {
    private final Militiaman mob;
    private ServerPlayer target;

    public ArrestGoal(Militiaman mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount % 10 != 0) return false;
        double range = CityData.get((ServerLevel) mob.level()).current() == DayEvent.RAID ? 36.0 : 24.0;
        for (Player p : mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(range, 8.0, range))) {
            if (!(p instanceof ServerPlayer sp) || sp.isCreative() || sp.isSpectator()) continue;
            if (!sees(sp)) continue;
            if (Crime.reason(sp) != null) {
                target = sp;
                return true;
            }
        }
        return false;
    }

    /** Патрульный видит игрока: в прямой видимости и примерно перед собой. */
    private boolean sees(Player p) {
        if (!mob.hasLineOfSight(p)) return false;
        Vec3 dir = p.position().subtract(mob.position()).normalize();
        return mob.getLookAngle().dot(dir) > -0.2;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && Crime.reason(target) != null
                && mob.distanceToSqr(target) < 40 * 40;
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(target, 1.35);
    }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(target, 30f, 30f);
        if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(target, 1.35);
        if (mob.distanceToSqr(target) < 4.0) {
            Crime.arrest(target, Crime.reason(target));
            target = null;
        }
    }

    @Override
    public void stop() {
        target = null;
        mob.getNavigation().stop();
    }
}
