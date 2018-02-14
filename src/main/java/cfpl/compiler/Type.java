package cfpl.compiler;

public enum Type {
    INT(1), FLOAT(2), CHAR(1), BOOL(1), STRING(1);

    public final int size;

    private Type(int size) {
        this.size = size;
    }
}
