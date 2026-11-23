package com.sovok.city.item;

import com.sovok.city.SovokCity;
import com.sovok.city.block.ModBlocks;
import com.sovok.city.entity.ModEntities;
import com.sovok.city.system.PlayerStats;
import com.sovok.city.system.Survival;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SovokCity.MODID);

    public static final DeferredItem<Item> RUBLE = ITEMS.registerSimpleItem("ruble");
    public static final DeferredItem<Item> COUPON = ITEMS.registerSimpleItem("coupon");

    public static final DeferredItem<Item> BLACK_BREAD = ITEMS.registerSimpleItem("black_bread",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build()));
    public static final DeferredItem<Item> TUSHONKA = ITEMS.registerSimpleItem("tushonka",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build()));

    public static final DeferredItem<Item> MUG = ITEMS.register("mug", () -> new MugItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BOILED_WATER = ITEMS.register("boiled_water", () -> new UseItem(
            new Item.Properties().stacksTo(16), UseAnim.DRINK, 32,
            p -> PlayerStats.addThirst(p, 40), () -> new ItemStack(Items.GLASS_BOTTLE)));

    public static final DeferredItem<Item> KOMPOT = ITEMS.register("kompot", () -> new UseItem(
            new Item.Properties().stacksTo(16), UseAnim.DRINK, 32,
            p -> { PlayerStats.addThirst(p, 30); p.getFoodData().eat(2, 0.3f); },
            () -> new ItemStack(Items.GLASS_BOTTLE)));

    public static final DeferredItem<Item> VODKA = ITEMS.register("vodka", () -> new UseItem(
            new Item.Properties().stacksTo(8), UseAnim.DRINK, 32,
            p -> {
                PlayerStats.setBody(p, PlayerStats.body(p) + 1.0);
                PlayerStats.addThirst(p, -8);
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200));
            }, () -> new ItemStack(Items.GLASS_BOTTLE)));

    public static final DeferredItem<Item> TABLETS = ITEMS.register("tablets", () -> new UseItem(
            new Item.Properties().stacksTo(16), UseAnim.EAT, 24,
            Survival::cureAll, () -> ItemStack.EMPTY));

    public static final DeferredItem<Item> MILITIAMAN_SPAWN_EGG = ITEMS.register("militiaman_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MILITIAMAN, 0x4A5A3A, 0xC8B040, new Item.Properties()));

    // Блоки-предметы
    public static final DeferredItem<?> RATION_POINT_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RATION_POINT);
    public static final DeferredItem<?> FOOD_STORE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.FOOD_STORE);
    public static final DeferredItem<?> BLACK_MARKET_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLACK_MARKET);
    public static final DeferredItem<?> SLOT_MACHINE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SLOT_MACHINE);
    public static final DeferredItem<?> CROUPIER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CROUPIER);
}
