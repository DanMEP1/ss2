package com.sovok.city.net;

/** Клиентская копия статов (только примитивы — безопасно загружать на сервере). */
public final class ClientStats {
    public static volatile int thirst = 100;
    public static volatile int body10 = 366;
    public static volatile int coldSecs = 0;
    public static volatile int dysSecs = 0;
    public static volatile int jailSecs = 0;
    public static volatile int alert = 0;
    public static volatile int rating = 50;
    public static volatile int event = 0;

    private ClientStats() {}
}
