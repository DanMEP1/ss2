package com.sovok.city.entity;

import com.sovok.city.system.Econ;
import com.sovok.city.system.PlayerStats;
import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** Социальный рейтинг 0: милиция гоняется за игроком и избивает его (но не добивает). */
public class BeatGoal extends Goal {
    private final Militiaman mob;
    private ServerPlayer target;
    private int cooldown;

    public BeatGoal(Militiaman mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean valid(ServerPlayer p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator() && PlayerStats.rating(p) <= 0
                && PlayerStats.jailUntil(p) <= p.serverLevel().getGameTime();
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount % 10 != 0) return false;
        for (Player p : mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(20.0, 8.0, 20.0))) {
            if (p instanceof ServerPlayer sp && valid(sp) && mob.hasLineOfSight(sp)) {
                target = sp;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && valid(target) && mob.distanceToSqr(target) < 30 * 30;
    }

    @Override
    public void start() { mob.getNavigation().moveTo(target, 1.3); }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(target, 30f, 30f);
        if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(target, 1.3);
        if (cooldown > 0) cooldown--;
        if (cooldown == 0 && mob.distanceToSqr(target) < 3.5 * 3.5) {
            mob.swing(InteractionHand.MAIN_HAND);
            if (target.getHealth() > 6.0F) {
                target.hurt(mob.level().damageSources().mobAttack(mob), 2.0F);
            }
            target.knockback(0.4, mob.getX() - target.getX(), mob.getZ() - target.getZ());
            Econ.msg(target, "msg.sovokcity.beaten");
            cooldown = 20;
        }
    }

    @Override
    public void stop() {
        target = null;
        mob.getNavigation().stop();
    }
}
