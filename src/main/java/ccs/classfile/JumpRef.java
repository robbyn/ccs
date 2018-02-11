package ccs.classfile;

public class JumpRef extends LabelRef {
    private int jumpLocation;

    public JumpRef(int jumpLocation) {
        this.jumpLocation = jumpLocation;
    }

    public void fixup(CodeBuilder cb, int location) {
        cb.fixupShort(jumpLocation+1, location-jumpLocation);
    }
}
