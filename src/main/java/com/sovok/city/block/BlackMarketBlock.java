package com.sovok.city.block;

import com.sovok.city.item.ModItems;
import com.sovok.city.system.Econ;
import com.sovok.city.system.PlayerStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;
import java.util.function.Supplier;

/** Чёрный рынок (спекулянт): Shift+ПКМ — выбрать товар, ПКМ — купить за рубли. */
public class BlackMarketBlock extends Block {
    private record Offer(Supplier<? extends Item> item, int price) {}

    private static final List<Offer> OFFERS = List.of(
            new Offer(ModItems.TUSHONKA, 8),
            new Offer(ModItems.BOILED_WATER, 4),
            new Offer(ModItems.TABLETS, 15),
            new Offer(ModItems.COUPON, 6),
            new Offer(ModItems.VODKA, 10),
            new Offer(ModItems.MUG, 5));

    public BlackMarketBlock(Properties p) { super(p); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int idx = Math.floorMod(PlayerStats.order(player), OFFERS.size());
        if (player.isShiftKeyDown()) {
            idx = (idx + 1) % OFFERS.size();
            PlayerStats.setOrder(player, idx);
            Offer o = OFFERS.get(idx);
            player.displayClientMessage(Component.translatable("msg.sovokcity.offer",
                    new ItemStack(o.item().get()).getHoverName(), Econ.scale(player, o.price())), true);
            return InteractionResult.CONSUME;
        }
        Offer o = OFFERS.get(idx);
        int price = Econ.scale(player, o.price());
        if (Econ.take(player, ModItems.RUBLE.get(), price)) {
            Econ.give(player, new ItemStack(o.item().get()));
            Econ.msg(player, "msg.sovokcity.deal");
        } else {
            player.displayClientMessage(Component.translatable("msg.sovokcity.not_enough",
                    new ItemStack(o.item().get()).getHoverName(), price), true);
        }
        return InteractionResult.CONSUME;
    }
}
