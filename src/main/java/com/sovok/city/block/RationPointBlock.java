package com.sovok.city.block;

import com.sovok.city.item.ModItems;
import com.sovok.city.system.Econ;
import com.sovok.city.system.PlayerStats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Пункт выдачи талонов: 3 талона на человека в игровые сутки. */
public class RationPointBlock extends Block {
    public RationPointBlock(Properties p) { super(p); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        long day = level.getDayTime() / 24000L;
        if (PlayerStats.lastCouponDay(player) == day) {
            Econ.msg(player, "msg.sovokcity.coupons_already");
        } else {
            PlayerStats.setLastCouponDay(player, day);
            Econ.give(player, new ItemStack(ModItems.COUPON.get(), 3));
            Econ.msg(player, "msg.sovokcity.coupons_given");
        }
        return InteractionResult.CONSUME;
    }
}
