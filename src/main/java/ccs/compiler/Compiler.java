package ccs.compiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.classfile.AttributeInfo;
import org.tastefuljava.classfile.ByteCode;
import org.tastefuljava.classfile.ClassFile;
import org.tastefuljava.classfile.CodeBuilder;
import org.tastefuljava.classfile.CodeSegment;
import org.tastefuljava.classfile.ConstantPool;
import org.tastefuljava.classfile.Label;
import org.tastefuljava.classfile.MethodInfo;

public class Compiler {
    private static final Logger LOG
            = Logger.getLogger(Compiler.class.getName());

    private static final String SUFFIX = ".class";

    private final ConstantPool cp = new ConstantPool();
    private final CodeBuilder cb = new CodeBuilder(cp, 0);
    private CodeSegment code = cb;
    private final Deque<CodeSegment> segStack = new LinkedList<>();
    private final List<VariableDeclaration> varList = new ArrayList<>();
    private final Map<String,Variable> varMap = new HashMap<>();

    public void openSegment() {
        segStack.addFirst(code);
        code = cb.newSegment();
    }

    public void writeTo(File file) throws IOException {
        String fileName = file.getName();
        if (fileName.endsWith(SUFFIX)) {
            fileName = fileName.substring(
                    0, fileName.length() - SUFFIX.length());
        }
        ClassFile cf = new ClassFile(cp, fileName);
        cf.setMajorVersion((short)48);
        createMainMethod(cf);
        cf.store(file);
    }

    private void createMainMethod(ClassFile cf) throws IOException {
        cb.returnVoid();
        AttributeInfo code = new AttributeInfo(
                cp.addUtf8("Code"), cb.getBytes());
        MethodInfo mi = new MethodInfo(
                MethodInfo.ACC_PUBLIC|MethodInfo.ACC_STATIC,
                cp.addUtf8("main"), cp.addUtf8("([Ljava/lang/String;)V"));
        mi.addAttribute(code);
        cf.addMethod(mi);
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
            varMap.put(var.name, new Variable(type, addr));
            storeVar(type, addr);
        }
        varList.clear();
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

    public void literalInt(String s) {
        code.pushInt(Integer.parseInt(s));
    }

    public void literalFloat(String s) {
        code.pushInt(Integer.parseInt(s));
    }

    public void literalChar(String s) {
        code.pushInt(Strings.unescapeChar(s));
    }

    public void literalString(String s) {
        code.pushString(Strings.unescape(s));
    }

    private void storeVar(Type type, int addr) {
        switch (type) {
            case BOOL:
            case CHAR:
            case INT:
                code.storeInt(addr);
                break;
            case FLOAT:
                code.storeDouble(addr);
                break;
            case STRING:
                code.storeRef(addr);
                break;
        }
    }

    public Type loadVar(String name) {
        Variable v = varMap.get(name);
        if (v == null) {
            LOG.log(Level.SEVERE, "Variable not found: {0}", name);
            return Type.INT;
        }
        loadVar(v.type, v.addr);
        return v.type;
    }

    private void loadVar(Type type, int addr) {
        switch (type) {
            case BOOL:
            case CHAR:
            case INT:
                code.loadInt(addr);
                break;
            case FLOAT:
                code.loadDouble(addr);
                break;
            case STRING:
                code.loadRef(addr);
                break;
        }
    }

    public void neg(Type type) {
        switch (type) {
            case BOOL:
            case CHAR:
            case INT:
                code.negInt();
                break;
            case FLOAT:
                code.negDouble();
                break;
            case STRING:
                LOG.severe("Cannot negate a string");
                break;
        }
    }

    public void output(Type type) {
        convert(type, Type.STRING);
        code.getStatic("java/lang/System", "out", "Ljava/io/PrintStream;");
        code.swap();
        code.invokeVirtual(
                "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    }
}
