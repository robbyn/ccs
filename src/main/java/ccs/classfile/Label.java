package ccs.classfile;

public class Label {
    private boolean defined;
    private int location;
    private LabelRef refs[];

    public Label() {
    }

    public boolean isDefined() {
        return defined;
    }

    public int getLocation() {
        return location;
    }

    void define(int newLoc) {
        defined = true;
        location = newLoc;
    }

    void fixupRefs(CodeBuilder cb) {
        if (refs != null) {
            for (int i = 0; i < refs.length; ++i) {
                refs[i].fixup(cb, location);
            }
        }
    }

    void addRef(LabelRef ref) {
        int len = refs == null ? 0 : refs.length;
        LabelRef refs2[] = new LabelRef[len+1];
        if (len > 0) {
            System.arraycopy(refs, 0, refs2, 0, len);
        }
        refs = refs2;
        refs[len] = ref;
    }
}
