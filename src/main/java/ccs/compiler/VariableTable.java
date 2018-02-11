package ccs.compiler;

import java.util.HashMap;
import java.util.Map;

public class VariableTable {
    private final Map<String,Variable> map = new HashMap<>();

    public int getSize() {
        return map.size();
    }

    public boolean exists(String name) {
        return map.containsKey(name);
    }

    public Variable lookup(String name) {
        return map.get(name);
    }

    public Variable create(String name, Type type) {
        if (exists(name)) {
            throw new IllegalArgumentException("Variable " + name
                    + " already declared");
        }
        Variable result = new Variable(type, map.size());
        map.put(name, result);
        return result;
    }
}
