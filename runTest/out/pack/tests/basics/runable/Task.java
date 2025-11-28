/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.runable;

import java.util.concurrent.RejectedExecutionException;
import pack.tests.basics.runable.Exec;
import pack.tests.basics.runable.Pool;

public class Task {
    public void run() throws Exception {
        Exec e1 = new Exec(2);
        Exec e2 = new Exec(3);
        Exec e3 = new Exec(100);
        try {
            Pool.tpe.submit(e2::doAdd);
            try {
                Thread.sleep(50L);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
            Pool.tpe.submit(() -> {
                int ix = Exec.i;
                e1.doAdd();
                Exec.i += ix;
            });
            try {
                Thread.sleep(50L);
            }
            catch (InterruptedException interruptedException) {
            }
            Pool.tpe.submit(e3::doAdd);
        }
        catch (RejectedExecutionException e) {
            Exec.i += 10;
        }
        Thread.sleep(300L);
        if (Exec.i == 30) {
            System.out.println("PASS");
        } else {
            System.out.println("FAIL");
        }
    }
}

