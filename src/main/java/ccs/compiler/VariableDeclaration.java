package ccs.compiler;

import org.tastefuljava.classfile.CodeSegment;

public class VariableDeclaration {
    public final String name;
    public final Type type;
    public final CodeSegment initValue;
    public final VariableDeclaration next;

    public VariableDeclaration(String name, Type type,
            CodeSegment initValue, VariableDeclaration next) {
        this.name = name;
        this.type = type;
        this.initValue = initValue;
        this.next = next;
    }
}
