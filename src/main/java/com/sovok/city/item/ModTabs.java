package com.sovok.city.item;

import com.sovok.city.SovokCity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.ItemStack;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SovokCity.MODID);

    public static final java.util.function.Supplier<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sovokcity"))
                    .icon(() -> new ItemStack(ModItems.RUBLE.get()))
                    .displayItems((params, out) -> ModItems.ITEMS.getEntries().forEach(e -> out.accept(e.get())))
                    .build());
}
