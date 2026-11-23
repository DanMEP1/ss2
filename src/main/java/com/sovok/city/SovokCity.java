package com.sovok.city;

import com.sovok.city.block.ModBlocks;
import com.sovok.city.entity.ModEntities;
import com.sovok.city.item.ModItems;
import com.sovok.city.item.ModTabs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(SovokCity.MODID)
public class SovokCity {
    public static final String MODID = "sovokcity";

    public SovokCity(IEventBus bus, ModContainer container) {
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModTabs.TABS.register(bus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
