package ccs.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class Context {
    private final BufferedReader in;
    private final PrintWriter out;
    private final Object[] vars;

    public Context(BufferedReader in, PrintWriter out, int nvars) {
        this.in = in;
        this.out = out;
        this.vars = new Object[nvars];
    }

    public String inputLine() throws IOException {
        return in.readLine();
    }

    public String[] inputValues() throws IOException {
        String line = in.readLine();
        return line == null ? null : line.trim().split("\\s*[,]\\s*");
    }

    public void outputLine(String line) {
        out.println(line);
    }

    public Object getVar(int addr) {
        return vars[addr];
    }

    public void setVar(int addr, Object newValue) {
        vars[addr] = newValue;
    }
}
