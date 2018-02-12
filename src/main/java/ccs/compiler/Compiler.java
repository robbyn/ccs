package ccs.compiler;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.tastefuljava.classfile.ByteCode;
import org.tastefuljava.classfile.CodeBuilder;
import org.tastefuljava.classfile.CodeSegment;
import org.tastefuljava.classfile.ConstantPool;
import org.tastefuljava.classfile.Label;

public class Compiler {
    private final ConstantPool cp = new ConstantPool();
    private final CodeBuilder cb = new CodeBuilder(cp, 1);
    private CodeSegment code = cb;
    private final Deque<CodeSegment> segStack = new LinkedList<>();
    private final List<VariableDeclaration> varList = new ArrayList<>();
    private final Map<String,Variable> varMap = new HashMap<>();

    public void openSegment() {
        segStack.addFirst(code);
        code = cb.newSegment();
    }

    public CodeSegment closeSegment() {
        CodeSegment result = code;
        code = segStack.removeFirst();
        return result;
    }

    public void addVar(String name, int line, int column) {
        varList.add(new VariableDeclaration(name, line, column));
    }

    public void addVar(String name, int line, int column,
            CodeSegment initExpr, Type initType) {
        varList.add(new VariableDeclaration(
                name, line, column, initExpr, initType));
    }

    public void declareAllVars(Type type) {
        for (VariableDeclaration var: varList) {
            int addr;
            switch (type) {
                case FLOAT:
                    addr = cb.newLocal2();
                    break;
                default:
                    addr = cb.newLocal();
                    break;
            }
            if (var.initType == null || var.initExpr == null) {
                defaultValue(type);
            } else {
                code.append(var.initExpr);
                convert(var.initType, type);
            }
        }
    }

    private void defaultValue(Type type) {
        switch (type) {
            case INT:
            case CHAR:
                code.pushInt(0);
                break;
            case FLOAT:
                code.pushDouble(0);
                break;
            case STRING:
                code.pushNull();
                break;
        }
    }

    private void convert(Type from, Type to) {
        if (from != to) {
            switch (from) {
                case BOOL:
                    if (to == Type.STRING) {
                        code.invokeStatic("java/lang/Boolean", "toString",
                                "(Z)Ljava/lang/String;");
                        break;
                    }
                case CHAR:
                    if (to == Type.STRING) {
                        code.invokeStatic("java/lang/Character", "toString",
                                "(C)Ljava/lang/String;");
                        break;
                    }
                case INT:
                    switch (to) {
                        case BOOL:
                            Label elseLabel = new Label();
                            Label endLabel = new Label();
                            code.jump(ByteCode.IFEQ, elseLabel);
                            code.pushInt(0);
                            code.jump(endLabel);
                            code.define(elseLabel);
                            code.pushInt(1);
                            code.define(endLabel);
                            break;
                        case CHAR:
                        case INT:
                        case FLOAT:
                            code.intToDouble();
                            break;
                        case STRING:
                            code.invokeStatic("java/lang/Integer", "toString",
                                    "(I)Ljava/lang/String;");
                            break;
                    }
                    break;
                case FLOAT:
                    switch (to) {
                        case BOOL:
                        case INT:
                        case CHAR:
                            code.doubleToInt();
                            convert(Type.INT, to);
                            break;
                        case STRING:
                            code.invokeStatic("java/lang/Double", "toString",
                                    "(D)Ljava/lang/String;");
                            break;
                            
                    }
                    break;
                case STRING:
                    switch (to) {
                        case BOOL:
                            code.invokeStatic("java/lang/Boolean",
                                    "parseBoolean", "(Ljava/lang/String;)Z");
                            break;
                        case CHAR:
                        case INT:
                            code.invokeStatic("java/lang/Integer", "parseInt",
                                    "(Ljava/lang/String;)I");
                            break;
                        case FLOAT:
                            code.invokeStatic("java/lang/Double", "parseDouble",
                                    "(Ljava/lang/String;)D");
                            break;
                    }
            }
        }
    }
}
