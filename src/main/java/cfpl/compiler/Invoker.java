package cfpl.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Invoker {
    private final Map<String,List<Function>> map = new HashMap<>();

    public static class Function {
        public final Type resultType;
        public final Type[] argTypes;
        public final Generator gen;

        public Function(Generator gen, Type resultType, Type... argTypes) {
            this.gen = gen;
            this.resultType = resultType;
            this.argTypes = argTypes;
        }
    }

    public void add(Generator gen, String name, Type resultType,
            Type... argTypes) {
        List<Function> list = map.get(name);
        if (list == null) {
            list = new ArrayList<>();
            map.put(name, list);
        }
        list.add(new Function(gen, resultType, argTypes));
    }

    public Function find(Converter conv, String name, Type... argTypes) {
        List<Function> list = map.get(name);
        if (list == null) {
            return null;
        }
        int argCount = argTypes.length;
        floop:
        for (Function fct: list) {
            if (fct.argTypes.length == argCount) {
                for (int i = 0; i < argCount; ++i) {
                    if (conv.getChain(argTypes[i], fct.argTypes[i]) == null) {
                        continue floop;
                    }
                }
                return fct;
            }
        }
        return null;
    }
}
