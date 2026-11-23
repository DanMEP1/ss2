package com.sovok.city.block;

import com.sovok.city.Config;
import com.sovok.city.item.ModItems;
import com.sovok.city.system.Econ;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Крупье подпольного казино: продаёт Upgrader за рубли (и выкупает за полцены с Shift). */
public class CroupierBlock extends Block {
    public CroupierBlock(Properties p) { super(p); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Item up = Econ.upgraderItem().orElse(null);
        if (up == null) {
            Econ.msg(player, "msg.sovokcity.no_upgrader_mod");
            return InteractionResult.CONSUME;
        }
        int basePrice = Config.UPGRADER_PRICE.get();
        int price = Econ.scale(player, basePrice);
        if (player.isShiftKeyDown() && player.getMainHandItem().is(up)) {
            player.getMainHandItem().shrink(1);
            Econ.giveRubles(player, Math.max(1, basePrice / 2));
            Econ.msg(player, "msg.sovokcity.croupier_buyback", Math.max(1, basePrice / 2));
        } else if (Econ.take(player, ModItems.RUBLE.get(), price)) {
            Econ.give(player, new ItemStack(up));
            Econ.msg(player, "msg.sovokcity.croupier_sold");
        } else {
            Econ.msg(player, "msg.sovokcity.croupier_price", price);
        }
        return InteractionResult.CONSUME;
    }
}
