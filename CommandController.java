package bearmetal;

import java.io.IOException;

import CLIProcess;
import CLIProcessBuilder;

/**
 * Wrapper for CLIProcess
 * Singleton used for executing shell commands
 * @author  Aidan McPeake
 */

public class CommandController {
    private static CommandController instance = null; 
  
    private CommandController() {} 
  
    public static CommandController getInstance() { 
        if (instance == null) 
            instance = new CommandController(); 
  
        return instance; 
    } 
    
    public CommandResult command(String command) throws IOException, InterruptedException { // Run string as shell command
    	try (CLIProcess process = new CLIProcessBuilder("bash", "-c", command).start()) {
            System.out.println("Sent command:\n" + command); // Log sent comands to the console
            process.waitFor();
            return new CommandResult(process);
        }
    }  
} 
