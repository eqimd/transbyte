// Polynom: P(x) = x^19 + x^18 + x^17 + x^14 + 1

class LFSR {

    public static boolean[] get_lfsr(boolean[] reg) {
        int len = 128;

        boolean[] output = new boolean[len];
        for (int i = 0; i < len; i++) {
            output[i] = shiftReg(reg);
        }

        return output;
    }

    static boolean shiftReg(boolean[] reg) {
        boolean x = reg[18];
        boolean y = reg[18]^reg[17]^reg[16]^reg[13];
        for (int j = 18; j > 0; --j) {
            reg[j] = reg[j - 1];
        }

        reg[0] = y;

        return x;
    }
}
