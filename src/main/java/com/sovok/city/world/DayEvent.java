package com.sovok.city.world;

import java.util.Locale;
import net.minecraft.util.RandomSource;

/** Событие дня: выбирается случайно в начале каждых игровых суток. */
public enum DayEvent {
    QUIET(3),        // обычный трудовой день
    FOOD(2),         // завоз продуктов: +2 талона
    RAID(2),         // облава: милиция зорче и чаще проверяет подвалы
    NO_HEATING(1),   // отключили отопление: в помещениях холодно
    FLU(1),          // эпидемия гриппа
    HEAT(1),         // аномальная жара: жажда вдвое быстрее
    SHORTAGE(1),     // дефицит: паёк за 2 талона
    HOLIDAY(1);      // праздник: +10 руб., +5 рейтинга, джекпоты вдвое

    public final int weight;

    DayEvent(int weight) { this.weight = weight; }

    public String key() { return name().toLowerCase(Locale.ROOT); }

    public static DayEvent roll(RandomSource r) {
        int total = 0;
        for (DayEvent e : values()) total += e.weight;
        int n = r.nextInt(total);
        for (DayEvent e : values()) {
            n -= e.weight;
            if (n < 0) return e;
        }
        return QUIET;
    }

    public static DayEvent byName(String s) {
        try { return valueOf(s); } catch (IllegalArgumentException ex) { return QUIET; }
    }
}
