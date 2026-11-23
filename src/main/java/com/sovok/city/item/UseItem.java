package com.sovok.city.item;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Универсальный употребляемый предмет (питьё, лекарство) с произвольным эффектом. */
public class UseItem extends Item {
    private final UseAnim anim;
    private final int duration;
    private final Consumer<ServerPlayer> effect;
    private final Supplier<ItemStack> remainder;

    public UseItem(Properties props, UseAnim anim, int duration, Consumer<ServerPlayer> effect,
                   Supplier<ItemStack> remainder) {
        super(props);
        this.anim = anim;
        this.duration = duration;
        this.effect = effect;
        this.remainder = remainder;
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return anim; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return duration; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp) {
            effect.accept(sp);
        }
        if (entity instanceof Player p && p.hasInfiniteMaterials()) return stack;
        stack.shrink(1);
        ItemStack rem = remainder.get();
        if (!rem.isEmpty() && entity instanceof Player p) {
            if (stack.isEmpty()) return rem;
            if (!p.getInventory().add(rem)) p.drop(rem, false);
        }
        return stack;
    }
}
