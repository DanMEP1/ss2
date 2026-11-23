package com.sovok.city.client;

import com.sovok.city.SovokCity;
import com.sovok.city.entity.ModEntities;
import com.sovok.city.net.ClientStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = SovokCity.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.MILITIAMAN.get(), MilitiamanRenderer::new);
    }

    @SubscribeEvent
    public static void hud(RegisterGuiLayersEvent e) {
        e.registerAboveAll(SovokCity.id("realism_hud"), (gfx, delta) -> draw(gfx));
    }

    private static void draw(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isCreative() || mc.player.isSpectator()) return;
        int x = 6, y = 6;

        // Жажда
        int th = ClientStats.thirst;
        bar(g, x, y, 60, th / 100f, th > 25 ? 0xFF3A9BDC : 0xFFDC3A3A);
        g.drawString(mc.font, Component.translatable("hud.sovokcity.thirst", th), x + 64, y, 0xFFFFFF, true);
        y += 12;

        // Температура тела
        double body = ClientStats.body10 / 10.0;
        int color = (body < 35.5 || body > 38.0) ? 0xFFDC3A3A : 0xFF5CCB5C;
        float frac = (float) Math.max(0, Math.min(1, (body - 32.0) / 10.0));
        bar(g, x, y, 60, frac, color);
        g.drawString(mc.font, String.format("%.1f °C", body), x + 64, y, 0xFFFFFF, true);
        y += 12;

        // Социальный рейтинг и событие дня
        int rt = ClientStats.rating;
        g.drawString(mc.font, Component.translatable("hud.sovokcity.rating", rt), x, y,
                rt <= 20 ? 0xFFFF5555 : 0xFFFFD755, true);
        y += 10;
        var evs = com.sovok.city.world.DayEvent.values();
        int ei = Math.max(0, Math.min(evs.length - 1, ClientStats.event));
        g.drawString(mc.font, Component.translatable("hud.sovokcity.today",
                Component.translatable("event.sovokcity." + evs[ei].key() + ".name")), x, y, 0xFFB0B0B0, true);
        y += 10;

        if (ClientStats.coldSecs > 0) {
            g.drawString(mc.font, Component.translatable("hud.sovokcity.cold"), x, y, 0xFFAADDFF, true);
            y += 10;
        }
        if (ClientStats.dysSecs > 0) {
            g.drawString(mc.font, Component.translatable("hud.sovokcity.dysentery"), x, y, 0xFFCCAA55, true);
            y += 10;
        }
        if (ClientStats.alert > 0) {
            g.drawString(mc.font, Component.translatable("hud.sovokcity.alert"), x, y, 0xFFFF4444, true);
            y += 10;
        }
        if (ClientStats.jailSecs > 0) {
            g.drawString(mc.font, Component.translatable("hud.sovokcity.jail", ClientStats.jailSecs), x, y, 0xFFFF8844, true);
        }
    }

    private static void bar(GuiGraphics g, int x, int y, int w, float frac, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + 9, 0xFF000000);
        g.fill(x, y, x + w, y + 8, 0xFF333333);
        g.fill(x, y, x + (int) (w * frac), y + 8, color);
    }
}
