package ccs.classfile;

public class Decode {

    public static void main(String[] args) {
        ClassFile cf = new ClassFile();

        try {
            cf.load(args[0]);
            cf.print(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
