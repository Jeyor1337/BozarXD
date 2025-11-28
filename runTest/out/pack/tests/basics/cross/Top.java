/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.cross;

import pack.tests.basics.cross.Abst1;
import pack.tests.basics.cross.Inte;

public class Top
extends Abst1
implements Inte {
    public void run() {
        if (this.add(1, 2) == 3 && this.mul(2, 3) == 6) {
            System.out.println("PASS");
            return;
        }
        System.out.println("FAIL");
    }

    @Override
    public int add(int a, int b) {
        return a + b;
    }
}

