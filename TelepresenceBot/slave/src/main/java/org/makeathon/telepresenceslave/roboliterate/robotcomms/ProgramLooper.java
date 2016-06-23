/**
 *
 * Copyright (C) 2013 Geoffrey Falk
 *
 */

package org.makeathon.telepresenceslave.roboliterate.robotcomms;

import org.makeathon.telepresenceslave.roboliterate.instructions.Program;

/**
 * Executes list of Instructions contained in RLit Program
 */
public interface ProgramLooper extends Runnable {

    void executeMainRoutineFromCurrentLine();

    void sendUIMessage(int status, int value);

    void setProgram(Program program);

}
