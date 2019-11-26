package bearmetal;

import CLIProcess;
import ProcessStreamsReader;

/**
 * Extends CLIProcess
 * Makes accessing relevant data members more convenient
 * @author  Aidan McPeake
 */

public class CommandResult {
	private final CLIProcess process;
	private final String output;
	private final String error;
	private final boolean succeeds;
	
	public CommandResult(CLIProcess process) {
		this.process = process;
		this.output = process.getOutput(ProcessStreamsReader.Type.STDOUT);
		this.error = process.getOutput(ProcessStreamsReader.Type.STDERROR);
		this.succeeds = process.getExitCode().equals(CLIProcess.EXIT_CODE.SUCCESS);
	}
	
	public CLIProcess getProcess() { return this.process; }
	public String getOutput() { return this.output; }
	public String getError() { return this.error; }
	public boolean succeeds() { return this.succeeds; }
}
