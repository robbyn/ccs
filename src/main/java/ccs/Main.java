package ccs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import ccs.parser.ParseException;
import ccs.parser.Parser;

public class Main {
    public static void main(String[] args) {
        try (InputStream in = new FileInputStream(args[0])) {
            Parser parser = new Parser(in);
            parser.parse();
        } catch (IOException | ParseException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
