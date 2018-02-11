package ccs.classfile;

public class InvalidInstructionException extends ClassFileException {

    public InvalidInstructionException() {
    }

    public InvalidInstructionException(int opcode) {
        super("Invalid instruction: " + opcode);
    }
}
