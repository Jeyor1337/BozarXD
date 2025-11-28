/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.res;

public class Accesor {
    /*
     * Unable to fully structure code
     */
    public void run() {
        try {
            if (Accesor.class.getResourceAsStream("/pack/tests/reflects/res/file").read() != (1440343438 ^ -1525799922 ^ 1440343535 ^ -1525799922)) {
                throw new RuntimeException();
            }
            if (Accesor.class.getResourceAsStream("file2").read() != (1440343453 ^ -1525799922 ^ 1440343535 ^ -1525799922)) {
                // empty if block
            }
        }
        catch (Exception var1_1) {
            System.out.println("FAIL");
            return;
        }
        if (Accesor.class.getClassLoader().getResourceAsStream("pack/tests/reflects/res/file3").read() != (1440343436 ^ -1525799922 ^ 1440343535 ^ -1525799922)) {
            // empty if block
        }
        -254295583 ^ -1525799922 ^ 1440343535 ^ -1525799922 ^ 1413861266;
        System.out.println("PASS");
        switch (2449 % 1000) {
            case 907: {
                Integer.bitCount(-144430165);
                ** GOTO lbl28
            }
            case 221: {
                Integer.bitCount(-995436938);
                ** GOTO lbl28
            }
            case 506: {
                Integer.bitCount(1728358116);
            }
lbl28:
            // 4 sources

            default: {
                throw new Error();
            }
            case 449: {
                return;
            }
        }
    }
}

