package com.sovok.city.item;

import com.sovok.city.system.PlayerStats;
import com.sovok.city.system.Survival;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Кружка: зачерпнуть и выпить воду из водоёма. Сырая вода рискованна (дизентерия). */
public class MugItem extends Item {
    public MugItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(4.5D, 0.0F, true);
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                if (!level.isClientSide && player instanceof ServerPlayer sp) {
                    PlayerStats.addThirst(sp, 15);
                    Survival.infectDysentery(sp, 0.40);
                    sp.getCooldowns().addCooldown(this, 20);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.sovokcity.drank_raw"), true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        return InteractionResultHolder.pass(stack);
    }
}
