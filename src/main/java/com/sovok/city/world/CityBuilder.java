package com.sovok.city.world;

import com.sovok.city.Config;
import com.sovok.city.block.ModBlocks;
import com.sovok.city.entity.Militiaman;
import com.sovok.city.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Строит «совковый» город-диктатуру 97x97 вокруг центра:
 * площадь с памятником Вождю, хрущёвки, продмаг+пункт талонов, легальное казино,
 * отделение милиции с камерой и подпольное казино в подвале хрущёвки.
 */
public class CityBuilder {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState CONCRETE = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
    private static final BlockState ROAD = Blocks.GRAY_CONCRETE.defaultBlockState();
    private static final BlockState PAVING = Blocks.POLISHED_ANDESITE.defaultBlockState();
    private static final BlockState FOUND = Blocks.COBBLESTONE.defaultBlockState();

    private final ServerLevel lv;
    private final int cx, y0, cz;

    private CityBuilder(ServerLevel lv, int cx, int y0, int cz) {
        this.lv = lv; this.cx = cx; this.y0 = y0; this.cz = cz;
    }

    /** Построить город, если ещё не построен. Центр — рядом с мировым спавном. */
    public static void ensureBuilt(ServerLevel overworld) {
        CityData data = CityData.get(overworld);
        if (data.built) return;
        BlockPos spawn = overworld.getSharedSpawnPos();
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        new CityBuilder(overworld, spawn.getX(), Math.max(overworld.getMinBuildHeight() + 12, y - 1), spawn.getZ()).build(data);
    }

    // ---------- helpers (координаты относительно центра) ----------
    private BlockPos p(int x, int y, int z) { return new BlockPos(cx + x, y, cz + z); }

    private void set(int x, int y, int z, BlockState s) { lv.setBlock(p(x, y, z), s, Block.UPDATE_CLIENTS); }

    /** Для панелей/решёток/заборов: пересчитать соединения с соседями. */
    private void setConn(int x, int y, int z, BlockState s) {
        BlockPos pos = p(x, y, z);
        lv.setBlock(pos, Block.updateFromNeighbourShapes(s, lv, pos), Block.UPDATE_ALL);
    }

    private void box(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, s);
    }

    private void lantern(int x, int y, int z) {
        set(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    private void bed(int x, int y, int z) {
        BlockState foot = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT);
        set(x, y, z, foot);
        set(x + 1, y, z, foot.setValue(BedBlock.PART, BedPart.HEAD));
    }

    // ---------- здания ----------
    /** Коробка здания с окнами; интерьер ground-этажа полый. doorX/doorZ — проём (ширина 2 по оси). */
    private void building(int x1, int z1, int x2, int z2, int height, BlockState wall, BlockState trim, int doorX, int doorZ, boolean doorOnX) {
        int top = y0 + height;
        box(x1, y0 + 1, z1, x2, top, z2, wall);
        box(x1, top, z1, x2, top, z2, trim);                // карниз/крыша
        box(x1 + 1, y0 + 1, z1 + 1, x2 - 1, y0 + 4, z2 - 1, AIR); // зал первого этажа
        for (int f = 0; f * 3 + 5 < height; f++) {          // окна
            int wy = y0 + 2 + f * 3;
            for (int x = x1 + 2; x <= x2 - 2; x += 3) {
                setConn(x, wy, z1, Blocks.GLASS_PANE.defaultBlockState());
                setConn(x, wy, z2, Blocks.GLASS_PANE.defaultBlockState());
            }
            if (f == 0) for (int z = z1 + 2; z <= z2 - 2; z += 3) {
                setConn(x1, wy, z, Blocks.GLASS_PANE.defaultBlockState());
                setConn(x2, wy, z, Blocks.GLASS_PANE.defaultBlockState());
            }
        }
        // проём
        if (doorOnX) box(doorX, y0 + 1, doorZ, doorX, y0 + 2, doorZ + 1, AIR);
        else box(doorX, y0 + 1, doorZ, doorX + 1, y0 + 2, doorZ, AIR);
        // освещение
        for (int x = x1 + 3; x <= x2 - 3; x += 5) lantern(x, y0 + 3, (z1 + z2) / 2);
        // окна: убрать панели, которые попали в проём
    }

    private void apartment(int x1, int z1, int x2, int z2, boolean northRow) {
        int doorZ = northRow ? z2 : z1;
        int xm = (x1 + x2) / 2;
        building(x1, z1, x2, z2, 16, CONCRETE, Blocks.POLISHED_ANDESITE.defaultBlockState(), xm, doorZ, false);
        // кровати и сундук в «коммуналке»
        int zi = northRow ? z1 + 1 : z2 - 1;
        bed(x1 + 1, y0 + 1, zi);
        bed(x1 + 1, y0 + 1, northRow ? z1 + 3 : z2 - 3);
        set(x2 - 1, y0 + 1, zi, Blocks.CHEST.defaultBlockState());
        set(x2 - 1, y0 + 1, northRow ? z1 + 3 : z2 - 3, Blocks.CRAFTING_TABLE.defaultBlockState());
    }

    private void lampPost(int x, int z) {
        for (int i = 1; i <= 4; i++) setConn(x, y0 + i, z, Blocks.OAK_FENCE.defaultBlockState());
        set(x, y0 + 5, z, Blocks.LANTERN.defaultBlockState());
    }

    // ---------- главная сборка ----------
    private void build(CityData data) {
        final int R = CityData.RADIUS;

        // 1. основание и расчистка
        box(-R, y0 - 8, -R, R, y0 - 1, R, FOUND);
        box(-R, y0, -R, R, y0, R, PAVING);
        box(-R, y0 + 1, -R, R, y0 + 26, R, AIR);

        // 2. дороги и площадь
        box(-R, y0, -3, R, y0, 3, ROAD);
        box(-3, y0, -R, 3, y0, R, ROAD);
        for (int x = -R; x <= R; x += 4) set(x, y0, 0, Blocks.WHITE_CONCRETE.defaultBlockState());
        for (int z = -R; z <= R; z += 4) set(0, y0, z, Blocks.WHITE_CONCRETE.defaultBlockState());
        box(-9, y0, -9, 9, y0, 9, Blocks.SMOOTH_STONE.defaultBlockState());
        box(-9, y0, -9, 9, y0, -9, Blocks.RED_CONCRETE.defaultBlockState());
        box(-9, y0, 9, 9, y0, 9, Blocks.RED_CONCRETE.defaultBlockState());
        box(-9, y0, -9, -9, y0, 9, Blocks.RED_CONCRETE.defaultBlockState());
        box(9, y0, -9, 9, y0, 9, Blocks.RED_CONCRETE.defaultBlockState());

        // 3. памятник Вождю
        box(-2, y0 + 1, -2, 2, y0 + 1, 2, Blocks.STONE_BRICKS.defaultBlockState());
        box(-1, y0 + 2, -1, 1, y0 + 2, 1, Blocks.STONE_BRICKS.defaultBlockState());
        box(0, y0 + 3, 0, 0, y0 + 8, 0, Blocks.QUARTZ_PILLAR.defaultBlockState());
        set(0, y0 + 9, 0, Blocks.GOLD_BLOCK.defaultBlockState());
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {0, 0}}) set(d[0], y0 + 10, d[1], Blocks.RED_WOOL.defaultBlockState());

        // 4. стена по периметру, ворота на дорогах, вышки по углам
        for (int i = -R; i <= R; i++) {
            for (int[] e : new int[][]{{i, -R}, {i, R}, {-R, i}, {R, i}}) {
                boolean gate = (Math.abs(e[0]) <= 3 && Math.abs(e[1]) == R) || (Math.abs(e[1]) <= 3 && Math.abs(e[0]) == R);
                if (gate) continue;
                box(e[0], y0 + 1, e[1], e[0], y0 + 5, e[1], Blocks.STONE_BRICKS.defaultBlockState());
                setConn(e[0], y0 + 6, e[1], Blocks.IRON_BARS.defaultBlockState());
            }
        }
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            int tx = sx * (R - 2), tz = sz * (R - 2);
            box(tx - 2, y0 + 1, tz - 2, tx + 2, y0 + 10, tz + 2, CONCRETE);
            box(tx - 2, y0 + 11, tz - 2, tx + 2, y0 + 11, tz + 2, Blocks.RED_CONCRETE.defaultBlockState());
            set(tx, y0 + 12, tz, Blocks.LANTERN.defaultBlockState());
        }

        // 5. фонари вдоль дорог
        for (int v : new int[]{-40, -28, -16, 16, 28, 40}) {
            lampPost(v, 4); lampPost(v, -4); lampPost(4, v); lampPost(-4, v);
        }

        // 6. жилые дома (хрущёвки)
        int[][] xs = {{-44, -30}, {-26, -12}, {12, 26}, {30, 44}};
        for (int[] r : xs) {
            apartment(r[0], -44, r[1], -34, true);
            apartment(r[0], 34, r[1], 44, false);
        }

        // 7. продмаг + пункт выдачи талонов
        building(6, -13, 18, -5, 6, Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(), Blocks.WHITE_CONCRETE.defaultBlockState(), 11, -5, false);
        set(9, y0 + 1, -12, ModBlocks.RATION_POINT.get().defaultBlockState());
        set(15, y0 + 1, -12, ModBlocks.FOOD_STORE.get().defaultBlockState());
        for (int x = 8; x <= 16; x++) if (x < 10 || x > 14) set(x, y0 + 1, -10, Blocks.SPRUCE_FENCE.defaultBlockState());
        data.ration = p(9, y0 + 1, -12);

        // 8. легальное казино «Красный Октябрь» (Спортлото)
        building(-18, -13, -6, -5, 6, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.RED_CONCRETE.defaultBlockState(), -13, -5, false);
        box(-17, y0 + 1, -12, -7, y0 + 1, -6, Blocks.RED_CARPET.defaultBlockState());
        for (int x : new int[]{-16, -14, -10, -8}) set(x, y0 + 1, -12, ModBlocks.SLOT_MACHINE.get().defaultBlockState());
        box(-18, y0 + 5, -13, -6, y0 + 5, -13, Blocks.GLOWSTONE.defaultBlockState()); // вывеска-неон
        data.casino = p(-12, y0 + 1, -9);

        // 9. отделение милиции с камерой
        building(6, 5, 18, 13, 6, Blocks.BLUE_CONCRETE.defaultBlockState(), Blocks.RED_CONCRETE.defaultBlockState(), 10, 5, false);
        for (int z = 8; z <= 12; z++) for (int y = 1; y <= 3; y++) setConn(13, y0 + y, z, Blocks.IRON_BARS.defaultBlockState());
        for (int x = 13; x <= 17; x++) for (int y = 1; y <= 3; y++) setConn(x, y0 + y, 8, Blocks.IRON_BARS.defaultBlockState());
        set(17, y0 + 1, 12, Blocks.HAY_BLOCK.defaultBlockState());
        data.jail = p(15, y0 + 1, 10);
        data.station = p(10, y0 + 1, 7);

        // 10. подпольное казино: подвал под южно-западной хрущёвкой, вход по лестнице из подъезда
        for (int i = 0; i <= 5; i++) {
            int sx = -42 + i;
            for (int y = y0 - i + 1; y <= y0 - i + 3; y++) set(sx, y, 39, AIR);
            set(sx, y0 - i, 39, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        }
        box(-36, y0 - 5, 35, -27, y0 - 1, 43, AIR);                  // зал
        box(-36, y0 - 6, 35, -27, y0 - 6, 43, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        box(-36, y0 - 5, 35, -27, y0 - 5, 43, AIR);
        for (int x = -35; x <= -28; x += 3) for (int z = 36; z <= 42; z += 3) lantern(x, y0 - 1, z);
        // карточные столы (бочки) и ковры
        for (int x = -34; x <= -29; x += 5) for (int z = 37; z <= 41; z += 4) {
            set(x, y0 - 5, z, Blocks.BARREL.defaultBlockState());
            set(x + 1, y0 - 5, z, Blocks.RED_CARPET.defaultBlockState());
        }
        set(-31, y0 - 5, 43, ModBlocks.CROUPIER.get().defaultBlockState());
        set(-29, y0 - 5, 43, ModBlocks.BLACK_MARKET.get().defaultBlockState());
        data.illMin = p(-42, y0 - 6, 35);
        data.illMax = p(-26, y0, 44);
        data.raidPoint = p(-43, y0 + 1, 39);
        data.hideout = p(-32, y0 - 5, 40);

        // 11. спавн, патруль, мировой спавн
        data.center = p(0, y0, 0);
        data.plaza = p(0, y0 + 1, 6);
        data.built = true;
        data.setDirty();
        lv.setDefaultSpawnPos(data.plaza, 0.0f);
        spawnPatrol();
    }

    /** Построить личную квартиру игрока (8x8, комната 6x6) по номеру. */
    public static void buildUnit(ServerLevel lv, CityData d, int idx) {
        new CityBuilder(lv, d.center.getX(), d.center.getY(), d.center.getZ()).unit(idx);
    }

    private void unit(int idx) {
        int[] b = Apartments.bounds(idx);
        int x1 = b[0], z1 = b[1], x2 = b[2], z2 = b[3];
        boolean north = b[4] == 1;
        BlockState wall = (idx % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState();
        box(x1, y0 + 1, z1, x2, y0 + 4, z2, wall);                       // коробка с крышей
        box(x1 + 1, y0 + 1, z1 + 1, x2 - 1, y0 + 3, z2 - 1, AIR);        // комната 6x6x3
        int doorZ = north ? z2 : z1;
        int farWall = north ? z1 : z2;
        int farZ = north ? z1 + 1 : z2 - 1;
        int sideZ = north ? farZ + 1 : farZ - 1;
        box(x1 + 3, y0 + 1, doorZ, x1 + 4, y0 + 2, doorZ, AIR);          // проём
        setConn(x1 + 2, y0 + 2, farWall, Blocks.GLASS_PANE.defaultBlockState());
        setConn(x1 + 5, y0 + 2, farWall, Blocks.GLASS_PANE.defaultBlockState());
        box(x1 + 2, y0 + 1, z1 + 2, x2 - 2, y0 + 1, z2 - 2, Blocks.RED_CARPET.defaultBlockState());
        bed(x1 + 1, y0 + 1, farZ);
        set(x2 - 1, y0 + 1, farZ, Blocks.CHEST.defaultBlockState());
        set(x2 - 1, y0 + 1, sideZ, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(x2 - 2, y0 + 1, farZ, Blocks.FURNACE.defaultBlockState());
        set(x1 + 3, y0 + 1, farZ, Blocks.BARREL.defaultBlockState());
        lantern(x1 + 3, y0 + 3, (z1 + z2) / 2);
    }

    private void spawnPatrol() {
        int n = Config.PATROL_COUNT.get();
        int[][] spots = {{0, 8}, {8, 0}, {0, -8}, {-8, 0}, {20, 0}, {-20, 0}, {0, 20}, {0, -20},
                {30, 0}, {-30, 0}, {0, 30}, {0, -30}, {40, 0}, {-40, 0}, {0, 40}, {0, -40}};
        for (int i = 0; i < n; i++) {
            int[] s = spots[i % spots.length];
            Militiaman m = ModEntities.MILITIAMAN.get().create(lv);
            if (m == null) continue;
            m.moveTo(cx + s[0] + 0.5, y0 + 1, cz + s[1] + 0.5, (i * 90) % 360, 0);
            dye(m, EquipmentSlot.HEAD, Items.LEATHER_HELMET);
            dye(m, EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE);
            dye(m, EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS);
            dye(m, EquipmentSlot.FEET, Items.LEATHER_BOOTS);
            m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
            m.setPersistenceRequired();
            lv.addFreshEntity(m);
        }
    }

    private static void dye(Militiaman m, EquipmentSlot slot, net.minecraft.world.item.Item item) {
        ItemStack st = new ItemStack(item);
        st.set(DataComponents.DYED_COLOR, new DyedItemColor(0x4A5A3A, false));
        m.setItemSlot(slot, st);
        m.setDropChance(slot, 0.0f);
    }
}
