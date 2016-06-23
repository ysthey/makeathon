/**
 *
 *  Copyright (c) 2013 Geoffrey Falk
 *
 */
package org.makeathon.telepresenceslave.roboliterate.instructions;

public interface Program {

    Instruction getInstructionAtLine(int i);

    int length();

    void addInstruction(Instruction rb);

}
