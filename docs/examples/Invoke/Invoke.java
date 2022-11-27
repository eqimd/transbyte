class Invoke {
    public static void inv1() {
        inv2();
        return;
    }

    static void inv2() {
        return;
    }
}
