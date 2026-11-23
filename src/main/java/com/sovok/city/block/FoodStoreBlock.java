package com.sovok.city.block;

import com.sovok.city.item.ModItems;
import com.sovok.city.system.Econ;
import com.sovok.city.world.CityData;
import com.sovok.city.world.DayEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Продмаг: 1 талон = 1 продукт. В дефицит и при низком соц. рейтинге талонов нужно больше. */
public class FoodStoreBlock extends Block {
    public FoodStoreBlock(Properties p) { super(p); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int base = CityData.get((ServerLevel) level).current() == DayEvent.SHORTAGE ? 2 : 1;
        int need = Econ.scale(player, base);
        if (!Econ.take(player, ModItems.COUPON.get(), need)) {
            Econ.msg(player, "msg.sovokcity.need_coupons", need);
            return InteractionResult.CONSUME;
        }
        Item[] goods = { ModItems.BLACK_BREAD.get(), ModItems.BLACK_BREAD.get(), ModItems.TUSHONKA.get(), ModItems.KOMPOT.get() };
        Econ.give(player, new ItemStack(goods[level.random.nextInt(goods.length)]));
        Econ.msg(player, "msg.sovokcity.bought_ration");
        return InteractionResult.CONSUME;
    }
}
