package com.sovok.city.net;

import com.sovok.city.SovokCity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = SovokCity.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Net {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");
        reg.playToClient(StatsPayload.TYPE, StatsPayload.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            ClientStats.thirst = payload.thirst();
            ClientStats.body10 = payload.body10();
            ClientStats.coldSecs = payload.coldSecs();
            ClientStats.dysSecs = payload.dysSecs();
            ClientStats.jailSecs = payload.jailSecs();
            ClientStats.alert = payload.alert();
            ClientStats.rating = payload.rating();
            ClientStats.event = payload.event();
        }));
    }

    public static void send(ServerPlayer p, StatsPayload payload) {
        PacketDistributor.sendToPlayer(p, payload);
    }
}
