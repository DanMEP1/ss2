package com.sovok.city.net;

import com.sovok.city.SovokCity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Синхронизация параметров реализма и города с клиентом для HUD. */
public record StatsPayload(int thirst, int body10, int coldSecs, int dysSecs, int jailSecs, int alert,
                           int rating, int event) implements CustomPacketPayload {

    public static final Type<StatsPayload> TYPE = new Type<>(SovokCity.id("stats"));

    public static final StreamCodec<ByteBuf, StatsPayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeInt(v.thirst); buf.writeInt(v.body10); buf.writeInt(v.coldSecs); buf.writeInt(v.dysSecs);
                buf.writeInt(v.jailSecs); buf.writeInt(v.alert); buf.writeInt(v.rating); buf.writeInt(v.event);
            },
            buf -> new StatsPayload(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
