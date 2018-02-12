package ccs.compiler;

import org.tastefuljava.classfile.CodeSegment;

class VariableDeclaration {
    public final String name;
    public final CodeSegment initExpr;
    public final Type initType;

    public VariableDeclaration(String name) {
        this(name, null, null);
    }

    public VariableDeclaration(String name, CodeSegment initExpr,
            Type initType) {
        this.name = name;
        this.initExpr = initExpr;
        this.initType = initType;
    }
}
