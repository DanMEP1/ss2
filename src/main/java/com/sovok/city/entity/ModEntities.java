package com.sovok.city.entity;

import com.sovok.city.SovokCity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = SovokCity.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SovokCity.MODID);

    public static final Supplier<EntityType<Militiaman>> MILITIAMAN = ENTITIES.register("militiaman",
            () -> EntityType.Builder.of(Militiaman::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("militiaman"));

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(MILITIAMAN.get(), Militiaman.createAttributes().build());
    }
}
