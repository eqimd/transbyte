class A5_1 {
    public static boolean[] generate(boolean[] regA, boolean[] regB, boolean[] regC) {
        int len = 128;

        boolean[] result = new boolean[len];

        int midA = 8;
        int midB = 10;
        int midC = 10;
        boolean maj;

        for (int i = 0; i < len; i++) {
            maj = majority(regA[midA], regB[midB], regC[midC]);

            if (!(maj^regA[midA])) {
                shift_rslosA(regA);
            }

            if (!(maj^(regB[midB]))) {
                shift_rslosB(regB);
            }

            if(!(maj^(regC[midC]))) {
                shift_rslosC(regC);
            }

            result[i] = regA[18]^regB[21]^regC[22];
        }

        return result;
    }

    public static boolean majority(boolean A, boolean B, boolean C) {
        return A&B | A&C | B&C;
    }

    public static boolean shift_rslosA(boolean[] reg) {
        boolean x = reg[18];
        boolean y = reg[18]^reg[17]^reg[16]^reg[13];

        for (int j = 18; j > 0; j--) {
            reg[j] = reg[j-1];
        }

        reg[0] = y;

        return x;
    }

    public static boolean shift_rslosB(boolean[] reg) {
        boolean x = reg[21];
        boolean y = reg[21]^reg[20];

        for (int j = 21; j > 0; j--) {
            reg[j] = reg[j-1];
        }

        reg[0] = y;

        return x;
    }

    public static boolean shift_rslosC(boolean[] reg) {
        boolean x = reg[22];
        boolean y = reg[22]^reg[21]^reg[20]^reg[7];

        for (int j = 22; j > 0; j--) {
            reg[j] = reg[j-1];
        }

        reg[0] = y;

        return x;
    }
}