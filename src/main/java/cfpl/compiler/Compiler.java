package cfpl.compiler;

import cfpl.compiler.Invoker.Call;
import cfpl.compiler.Invoker.Function;
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
    private static final Converter STRICT;
    private static final Converter LOOSE;
    private static final Invoker INVOKER;

    private final ConstantPool cp = new ConstantPool();
    private final CodeBuilder cb = new CodeBuilder(cp, 0);
    private CodeSegment code = cb;
    private final Deque<CodeSegment> segStack = new LinkedList<>();
    private final List<VariableDeclaration> varList = new ArrayList<>();
    private final Map<String,Variable> varMap = new HashMap<>();
    private Call call;

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
        AttributeInfo ai = new AttributeInfo(
                cp.addUtf8("Code"), cb.getBytes());
        MethodInfo mi = new MethodInfo(
                MethodInfo.ACC_PUBLIC|MethodInfo.ACC_STATIC,
                cp.addUtf8("main"), cp.addUtf8("([Ljava/lang/String;)V"));
        mi.addAttribute(ai);
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
                convert(LOOSE, var.initType, type);
            }
            varMap.put(var.name, new Variable(type, addr));
            storeVar(type, addr);
        }
        varList.clear();
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

    public Type loadVar(String name) {
        Variable v = varMap.get(name);
        if (v == null) {
            LOG.log(Level.SEVERE, "Variable not declared: {0}", name);
            return Type.INT;
        }
        loadVar(v.type, v.addr);
        return v.type;
    }

    public void storeVar(Type type, String name) {
        Variable v = varMap.get(name);
        if (v == null) {
            LOG.log(Level.SEVERE, "Variable not declared: {0}", name);
            return;
        }
        convert(LOOSE, type, v.type);
        storeVar(v.type, v.addr);
    }

    public void startCall() {
        call = INVOKER.startCall(code, call);
    }

    public Type endCall(String name) {
        Type result = call.invoke(name);
        call = call.link;
        return result;
    }

    public void startArg() {
        code = call.startArg();
    }

    public void endArg(Type t) {
        code = call.endArg(t);
    }

    public void startOp2(Type type1) {
        startCall();
        startArg();
        endArg(type1);
        startArg();
    }

    public Type endOp2(String op, Type type2) {
        endArg(type2);
        return endCall(op);
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
        convert(LOOSE, type, Type.STRING);
        code.getStatic("java/lang/System", "out", "Ljava/io/PrintStream;");
        code.swap();
        code.invokeVirtual(
                "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    }

    public Label startIf(Type type) {
        Label label = new Label();
        convert(LOOSE, type, Type.BOOL);
        code.jump(ByteCode.IFEQ, label);
        return label;
    }

    public Label startElse(Label label) {
        Label endLabel = new Label();
        code.jump(endLabel);
        code.define(label);
        return endLabel;
    }

    public void endIf(Label label) {
        code.define(label);
    }

    public Label startWhile() {
        Label label = new Label();
        code.define(label);
        return label;
    }

    public Label whileCond(Type type) {
        Label label = new Label();
        convert(LOOSE, type, Type.BOOL);
        code.jump(ByteCode.IFEQ, label);
        return label;
    }

    public void endWhile(Label begin, Label end) {
        code.jump(begin);
        code.define(end);
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

    private void convert(Converter conv, Type from, Type to) {
        if (from != to) {
            Converter.Chain chain = conv.getChain(from, to);
            if (chain == null) {
                LOG.log(Level.SEVERE, "No implicit conversion from {0} to {1}",
                        new Object[]{from, to});
            } else {
                chain.apply(code);
            }
        }
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

    private static void compare(CodeSegment code, int opCode) {
        Label elseLabel = new Label();
        Label endLabel = new Label();
        code.jump(opCode, elseLabel);
        code.pushInt(0);
        code.jump(endLabel);
        code.define(elseLabel);
        code.pushInt(1);
        code.define(endLabel);
    }

    static {
        STRICT = new Converter();
        STRICT.add(Type.BOOL, Type.CHAR, (code)->{});
        STRICT.add(Type.CHAR, Type.INT, (code)->{});
        STRICT.add(Type.INT, Type.FLOAT, (code)->{
                code.intToDouble();
            });

        STRICT.add(Type.BOOL, Type.STRING, (code)->{
                code.invokeStatic("java/lang/Boolean", "toString",
                        "(Z)Ljava/lang/String;");
            });
        STRICT.add(Type.CHAR, Type.STRING, (code)->{
                code.invokeStatic("java/lang/Character", "toString",
                        "(C)Ljava/lang/String;");
            });
        STRICT.add(Type.INT, Type.STRING, (code)->{
                code.invokeStatic("java/lang/Integer", "toString",
                        "(I)Ljava/lang/String;");
            });
        STRICT.add(Type.FLOAT, Type.STRING, (code)->{
                code.invokeStatic("java/lang/Double", "toString",
                        "(D)Ljava/lang/String;");
            });
        LOOSE = new Converter(STRICT);
        LOOSE.add(Type.FLOAT, Type.INT, (code) -> {
            code.doubleToInt();
        });
        LOOSE.add(Type.INT, Type.CHAR, (code)->{});
        LOOSE.add(Type.INT, Type.BOOL, (code)->{
            compare(code, ByteCode.IFEQ);
        });
        LOOSE.add(Type.STRING, Type.BOOL, (code)->{
            code.invokeStatic("java/lang/Boolean",
                    "parseBoolean", "(Ljava/lang/String;)Z");
        });
        LOOSE.add(Type.STRING, Type.INT, (code)->{
            code.invokeStatic("java/lang/Integer", "parseInt",
                    "(Ljava/lang/String;)I");
        });
        LOOSE.add(Type.STRING, Type.FLOAT, (code)->{
            code.invokeStatic("java/lang/Double", "parseDouble",
                    "(Ljava/lang/String;)D");
        });
        INVOKER = new Invoker(STRICT);
        INVOKER.add((code)->{
            code.invokeVirtual("java/lang/String", "concat",
                    "(Ljava/lang/String;)Ljava/lang/String;");
        }, "&", Type.STRING, Type.STRING, Type.STRING);
        INVOKER.add((code)->{
            code.andInt();
        }, "AND", Type.BOOL, Type.BOOL, Type.BOOL);
        INVOKER.add((code)->{
            code.andInt();
        }, "OR", Type.BOOL, Type.BOOL, Type.BOOL);
        INVOKER.add((code)->{
            code.addInt();
        }, "+", Type.INT, Type.INT, Type.INT);
        INVOKER.add((code)->{
            code.addDouble();
        }, "+", Type.FLOAT, Type.FLOAT, Type.FLOAT);
        INVOKER.add((code)->{
            code.subInt();
        }, "-", Type.INT, Type.INT, Type.INT);
        INVOKER.add((code)->{
            code.subDouble();
        }, "-", Type.FLOAT, Type.FLOAT, Type.FLOAT);
        INVOKER.add((code)->{
            code.mulInt();
        }, "*", Type.INT, Type.INT, Type.INT);
        INVOKER.add((code)->{
            code.mulDouble();
        }, "*", Type.FLOAT, Type.FLOAT, Type.FLOAT);
        INVOKER.add((code)->{
            code.divInt();
        }, "/", Type.INT, Type.INT, Type.INT);
        INVOKER.add((code)->{
            code.divDouble();
        }, "/", Type.FLOAT, Type.FLOAT, Type.FLOAT);
        INVOKER.add((code)->{
            code.remInt();
        }, "%", Type.INT, Type.INT, Type.INT);
        INVOKER.add((code)->{
            compare(code, ByteCode.IF_ICMPEQ);
        }, "==", Type.BOOL, Type.INT, Type.INT);
        INVOKER.add((code)->{
            code.cmplDouble();
            compare(code, ByteCode.IFEQ);
        }, "==", Type.BOOL, Type.FLOAT, Type.FLOAT);
        INVOKER.add((code)->{
            compare(code, ByteCode.IF_ICMPNE);
        }, "<>", Type.BOOL, Type.INT, Type.INT);
        INVOKER.add((code)->{
            code.cmplDouble();
            compare(code, ByteCode.IFNE);
        }, "<>", Type.BOOL, Type.FLOAT, Type.FLOAT);
        INVOKER.add((code)->{
            compare(code, ByteCode.IF_ICMPLT);
        }, "<", Type.BOOL, Type.INT, Type.INT);
        INVOKER.add((code)->{
            code.cmplDouble();
            compare(code, ByteCode.IFLT);
        }, "<", Type.BOOL, Type.FLOAT, Type.FLOAT);
        INVOKER.add((code)->{
            compare(code, ByteCode.IF_ICMPLE);
        }, "<=", Type.BOOL, Type.INT, Type.INT);
        INVOKER.add((code)->{
            code.cmplDouble();
            compare(code, ByteCode.IFLE);
        }, "<=", Type.BOOL, Type.FLOAT, Type.FLOAT);
        INVOKER.add((code)->{
            compare(code, ByteCode.IF_ICMPGT);
        }, ">", Type.BOOL, Type.INT, Type.INT);
        INVOKER.add((code)->{
            code.cmplDouble();
            compare(code, ByteCode.IFGT);
        }, ">", Type.BOOL, Type.FLOAT, Type.FLOAT);
        INVOKER.add((code)->{
            compare(code, ByteCode.IF_ICMPGE);
        }, ">=", Type.BOOL, Type.INT, Type.INT);
        INVOKER.add((code)->{
            code.cmplDouble();
            compare(code, ByteCode.IFGE);
        }, ">=", Type.BOOL, Type.FLOAT, Type.FLOAT);
    }
}
