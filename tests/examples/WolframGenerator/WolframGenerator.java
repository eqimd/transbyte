class WolframGenerator {

    private boolean[] ddd = new boolean[10];
    public static boolean[] generate(boolean[] reg) {
        int reg_len = 128;

        boolean[] buff = new boolean[reg_len];
        boolean[] result = new boolean[reg_len];

//        for (int i = 0; i < reg_len; i++) {
//            update(reg, buff);
//        }

        for (int i = 0; i < reg_len; i++) {
            update(reg, buff);
            result[i] = reg[0];
        }

        return result;
    }

    // One step of state updating
    public static void update(boolean[] reg, boolean[] buff) {
        int reg_len = 128;

        boolean left;
        boolean right;

        for (int i = 0; i < reg_len; i++) {
            if (i == 0) {
                left = reg[reg_len - 1];
            } else {
                left = reg[i - 1];
            }

            if (i + 1 == reg_len) {
                right = reg[0];
            } else {
                right = reg[i + 1];
            }

            buff[i] = left ^ (reg[i] | right);
        }

        for (int i = 0; i < reg_len; i++) {
            reg[i] = buff[i];
        }
    }
}