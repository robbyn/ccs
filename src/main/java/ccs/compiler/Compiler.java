package ccs.compiler;

import java.util.Deque;
import java.util.LinkedList;
import org.tastefuljava.classfile.CodeBuilder;
import org.tastefuljava.classfile.CodeSegment;
import org.tastefuljava.classfile.ConstantPool;

public class Compiler {
    private final ConstantPool cp = new ConstantPool();
    private final CodeBuilder cb = new CodeBuilder(cp, 1);
    private CodeSegment code = cb;
    private final Deque<CodeSegment> segStack = new LinkedList<>();

    public void openSegment() {
        segStack.addFirst(code);
        code = cb.newSegment();
    }

    public CodeSegment closeSegment() {
        CodeSegment result = code;
        code = segStack.removeFirst();
        return result;
    }
}
