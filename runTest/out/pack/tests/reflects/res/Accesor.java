/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.res;

public class Accesor {
    public void run() {
        try {
            if (Accesor.class.getResourceAsStream("/pack/tests/reflects/res/file").read() != 97) {
                throw new RuntimeException();
            }
            if (Accesor.class.getResourceAsStream("file2").read() != 114) {
                throw new RuntimeException();
            }
            if (Accesor.class.getClassLoader().getResourceAsStream("pack/tests/reflects/res/file3").read() != 99) {
                throw new RuntimeException();
            }
            System.out.println("PASS");
        }
        catch (Exception e) {
            System.out.println("FAIL");
        }
    }
}

