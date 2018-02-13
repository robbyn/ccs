package cfpl.compiler;

import org.tastefuljava.classfile.CodeSegment;

class VariableDeclaration {
    final String name;
    final int line;
    final int column;
    final CodeSegment initExpr;
    final Type initType;

    public VariableDeclaration(String name, int line, int column) {
        this(name, line, column, null, null);
    }

    public VariableDeclaration(String name, int line, int column,
            CodeSegment initExpr, Type initType) {
        this.name = name;
        this.line = line;
        this.column = column;
        this.initExpr = initExpr;
        this.initType = initType;
    }
}
