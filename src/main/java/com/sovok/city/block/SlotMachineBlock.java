package com.sovok.city.block;

import com.sovok.city.item.ModItems;
import com.sovok.city.system.Econ;
import com.sovok.city.world.CityData;
import com.sovok.city.world.DayEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Игровой автомат «Спортлото» в легальном клубе. 5 рублей за прокрутку. */
public class SlotMachineBlock extends Block {
    public static final int PRICE = 5;

    public SlotMachineBlock(Properties p) { super(p); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int price = Econ.scale(player, PRICE);
        if (!Econ.take(player, ModItems.RUBLE.get(), price)) {
            Econ.msg(player, "msg.sovokcity.slot_need", price);
            return InteractionResult.CONSUME;
        }
        int r = level.random.nextInt(100);
        int win = r < 60 ? 0 : r < 85 ? 5 : r < 95 ? 10 : r < 99 ? 25 : 100;
        if (CityData.get((ServerLevel) level).current() == DayEvent.HOLIDAY) win *= 2;
        if (win > 0) {
            Econ.giveRubles(player, win);
            Econ.msg(player, "msg.sovokcity.slot_win", win);
        } else {
            Econ.msg(player, "msg.sovokcity.slot_lose");
        }
        return InteractionResult.CONSUME;
    }
}
