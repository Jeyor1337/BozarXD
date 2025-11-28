/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.sub;

import pack.tests.basics.sub.SolAdd;

public class Solver {
    public Solver() {
        if (SolAdd.get() == 3) {
            System.out.println("PASS");
        } else {
            System.out.println("FAIL");
        }
    }
}

