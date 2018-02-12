package ccs.compiler;

import org.tastefuljava.classfile.CodeSegment;

public class VariableDeclaration {
    public final String name;
    public final CodeSegment initExpr;
    public final Type initType;
    public final VariableDeclaration next;

    public VariableDeclaration(String name, VariableDeclaration next) {
        this(name, null, null, next);
    }

    public VariableDeclaration(String name, CodeSegment initExpr, Type initType,
            VariableDeclaration next) {
        this.name = name;
        this.initExpr = initExpr;
        this.initType = initType;
        this.next = next;
    }
}
