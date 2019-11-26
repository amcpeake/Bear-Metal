package bearmetal;

public class APIAccessException extends Exception { // Custom exception containing error message and exit code
    	private int exitCode;
    	
        public APIAccessException(String errorMessage, int exitCode) {
            super(errorMessage);
            this.exitCode = exitCode;
        }

        public int getExitCode() { return exitCode; }
}
