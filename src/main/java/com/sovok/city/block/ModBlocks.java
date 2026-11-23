package com.sovok.city.block;

import com.sovok.city.SovokCity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SovokCity.MODID);

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 30.0f).sound(SoundType.METAL);
    }

    public static final DeferredBlock<Block> RATION_POINT = BLOCKS.register("ration_point", () -> new RationPointBlock(props()));
    public static final DeferredBlock<Block> FOOD_STORE = BLOCKS.register("food_store", () -> new FoodStoreBlock(props()));
    public static final DeferredBlock<Block> BLACK_MARKET = BLOCKS.register("black_market", () -> new BlackMarketBlock(props()));
    public static final DeferredBlock<Block> SLOT_MACHINE = BLOCKS.register("slot_machine", () -> new SlotMachineBlock(props().lightLevel(s -> 8)));
    public static final DeferredBlock<Block> CROUPIER = BLOCKS.register("croupier", () -> new CroupierBlock(props().lightLevel(s -> 6)));
}
