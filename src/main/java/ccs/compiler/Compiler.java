package ccs.compiler;

import ccs.compiler.Invoker.Function;
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
                convert(LOOSE, var.initType, type);
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

    private void convert(Converter conv, Type from, Type to) {
        if (from != to) {
            Converter.Chain chain = conv.getChain(from, to);
            if (chain == null) {
                LOG.severe("No implicit conversion from " + from + " to " + to);
            } else {
                chain.apply(code);
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

    public Type op2(String name, Type type1, Type type2, CodeSegment exp2) {
        Function fct = INVOKER.find(STRICT, name, type1, type2);
        if (fct == null) {
            LOG.log(Level.SEVERE,
                    "Operator {0} cannot be applied to types ({1},{2})",
                    new Object[]{name, type1, type2});
            return type1;
        }
        convert(STRICT, type1, fct.argTypes[0]);
        code.append(exp2);
        convert(STRICT, type2, fct.argTypes[1]);
        fct.gen.generate(code);
        return fct.resultType;
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
        convert(LOOSE, type, Type.STRING);
        code.getStatic("java/lang/System", "out", "Ljava/io/PrintStream;");
        code.swap();
        code.invokeVirtual(
                "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
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
            Label elseLabel = new Label();
            Label endLabel = new Label();
            code.jump(ByteCode.IFEQ, elseLabel);
            code.pushInt(0);
            code.jump(endLabel);
            code.define(elseLabel);
            code.pushInt(1);
            code.define(endLabel);
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
        INVOKER = new Invoker();
        INVOKER.add((code)->{
            code.invokeVirtual("java/lang/String", "concat",
                    "(Ljava/lang/String;)Ljava/lang/String;");
        }, "&", Type.STRING, Type.STRING, Type.STRING);
    }
}
