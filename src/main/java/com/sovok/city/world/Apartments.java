package com.sovok.city.world;

import com.sovok.city.SovokCity;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Личные квартиры: у каждого игрока своя комната 8x8, чужие не могут ломать/строить/открывать в ней. */
@EventBusSubscriber(modid = SovokCity.MODID)
public class Apartments {
    public static final int CAPACITY = 32;

    /** Координаты относительно центра города: {x1, z1, x2, z2, strip(0 юг / 1 север)}. */
    public static int[] bounds(int idx) {
        int strip = (idx / 16) % 2;
        int row = (idx % 16) / 8;
        int col = idx % 8;
        int x1 = col < 4 ? -44 + 9 * col : 7 + 9 * (col - 4);
        int z1 = strip == 0 ? (row == 0 ? 16 : 25) : (row == 0 ? -23 : -32);
        return new int[]{x1, z1, x1 + 7, z1 + 7, strip};
    }

    public static AABB box(CityData d, int idx) {
        int[] b = bounds(idx);
        int cx = d.center.getX(), y0 = d.center.getY(), cz = d.center.getZ();
        return new AABB(cx + b[0], y0 - 1, cz + b[1], cx + b[2] + 1, y0 + 6, cz + b[3] + 1);
    }

    /** Точка появления внутри квартиры (у двери). */
    public static BlockPos spawn(CityData d, int idx) {
        int[] b = bounds(idx);
        int z = b[4] == 0 ? b[1] + 1 : b[3] - 1;
        return new BlockPos(d.center.getX() + b[0] + 3, d.center.getY() + 1, d.center.getZ() + z);
    }

    public static float yaw(int idx) { return bounds(idx)[4] == 0 ? 0f : 180f; }

    /** Выдать квартиру игроку (строит её при первой выдаче). -1, если свободных нет. */
    public static int assign(ServerPlayer p) {
        ServerLevel ow = p.server.overworld();
        CityData d = CityData.get(ow);
        Integer idx = d.owners.get(p.getUUID());
        if (idx != null) return idx;
        if (d.nextUnit >= CAPACITY) return -1;
        int n = d.nextUnit++;
        d.owners.put(p.getUUID(), n);
        d.setDirty();
        CityBuilder.buildUnit(ow, d, n);
        return n;
    }

    private static boolean denied(net.minecraft.world.entity.player.Player p, BlockPos pos) {
        if (!(p instanceof ServerPlayer sp) || sp.hasPermissions(2) || sp.isCreative()) return false;
        CityData d = CityData.get(sp.serverLevel());
        if (!d.built || d.owners.isEmpty()) return false;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        for (Map.Entry<UUID, Integer> en : d.owners.entrySet()) {
            if (en.getKey().equals(sp.getUUID())) continue;
            if (box(d, en.getValue()).contains(x, y, z)) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (denied(e.getPlayer(), e.getPos())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof net.minecraft.world.entity.player.Player p && denied(p, e.getPos())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide()) return;
        if (denied(e.getEntity(), e.getPos())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
