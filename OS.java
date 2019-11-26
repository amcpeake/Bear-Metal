package bearmetal;

import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Instantiated by HiveHandler
 * Dynamically gathers and stores information
 * about snapshots available on disk 
 * @author  Aidan McPeake
 */

public class OS {
	private static CommandController controller = CommandController.getInstance();
	
	protected final String name; // Mapped device name
    protected final String path; // Path to mapped device (/dev/mapper/<Device Name>)
    
    private OS(String name)  {
        this.name = name;
        this.path = "/dev/mapper/" + name;
    }
    
    protected static OS get(String input) throws InterruptedException, IOException, APIAccessException {
    	return new OS(findOS(input).stream() // Get first result given input string, or throw exception if none found
    			.findFirst()
    			.orElseThrow(() -> new APIAccessException(input + " is not a valid OS name", 400)));
    }
    
    public static List<String> findOS(String keyword) throws InterruptedException, IOException { // Get a list of OS names which match a given keyword
        return Arrays.asList(controller.command("ls /dev/mapper").getOutput().split("\n")).stream()
        		.filter(image -> Objects.equals(keyword,  "") || image.toUpperCase().contains(keyword.toUpperCase()))
        		.collect(Collectors.toList());
    }
}
