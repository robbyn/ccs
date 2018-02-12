package ccs.compiler;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.tastefuljava.classfile.CodeBuilder;
import org.tastefuljava.classfile.CodeSegment;
import org.tastefuljava.classfile.ConstantPool;

public class Compiler {
    private final ConstantPool cp = new ConstantPool();
    private final CodeBuilder cb = new CodeBuilder(cp, 1);
    private CodeSegment code = cb;
    private final Deque<CodeSegment> segStack = new LinkedList<>();
    private final List<VariableDeclaration> varList = new ArrayList<>();
    private final Map<String,Variable> varMap = new HashMap<>();

    public void createSegment() {
        segStack.addFirst(code);
        code = cb.newSegment();
    }

    public CodeSegment closeSegment() {
        CodeSegment result = code;
        code = segStack.removeFirst();
        return result;
    }

    public void addVar(String name) {
        varList.add(new VariableDeclaration(name));
    }

    public void addVar(String name, CodeSegment initExpr, Type initType) {
        varList.add(new VariableDeclaration(name));
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
        }
    }
}
