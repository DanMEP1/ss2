package com.sovok.city.entity;

import com.sovok.city.world.CityData;
import com.sovok.city.world.DayEvent;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

/** Время от времени патруль заглядывает в подозрительные места (подвал с подпольным казино). */
public class RaidGoal extends Goal {
    private final Militiaman mob;
    private int ticks;

    public RaidGoal(Militiaman mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return mob.level() instanceof ServerLevel && mob.getRandom().nextInt(CityData.get((ServerLevel) mob.level()).current() == DayEvent.RAID ? 120 : 1200) == 0
                && CityData.get((ServerLevel) mob.level()).built;
    }

    @Override
    public void start() {
        ticks = 0;
        BlockPos t = CityData.get((ServerLevel) mob.level()).raidPoint;
        mob.getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, 1.0);
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < 600 && !mob.getNavigation().isDone();
    }

    @Override
    public void tick() { ticks++; }
}
