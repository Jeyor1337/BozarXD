/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.overwirte;

import pack.tests.basics.overwirte.Face;
import pack.tests.basics.overwirte.Super;

public class Sub
extends Super {
    @Override
    public void run() {
        Sub o = this;
        System.out.println(((Face)o).face(1));
    }

    @Override
    public String face(int i) {
        if (i == 1) {
            return "PASS";
        }
        return "FAIL";
    }
}

