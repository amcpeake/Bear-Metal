package bearmetal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import YMLConfiguration;

/**
 * Instantiated on startup, and accessed by HiveHandler
 * Parses drone information from yaml and provides wrapper for
 * drone control shell commands
 * @author  Aidan McPeake
 */

public class DroneController {
	private static CommandController controller = CommandController.getInstance();
    private static DroneController instance = null; 
    private ArrayList<Drone> drones;
    
    private DroneController() {
    	YMLConfiguration conf = YMLConfiguration.getConf(new File("Drones.yaml"));

    	this.drones = new ArrayList<Drone>(
    			conf.getList("drones").stream()
    			.map(drone -> new Drone((LinkedHashMap<String, Object>) drone))
    			.collect(Collectors.toList())
    	);
    } 
  
    protected static DroneController getInstance() { 
        if (instance == null) 
            instance = new DroneController(); 
  
        return instance; 
    }

    protected Drone get(String name) throws InterruptedException, IOException, APIAccessException {
    	return this.drones.stream()
    			.filter(drone -> Objects.equals(name.toUpperCase(), drone.name.toUpperCase()))
    			.findFirst()
    			.orElseThrow(() -> new APIAccessException(name + " is not a valid drone name", 400));
    }
    
    protected Drone get(int ID) throws InterruptedException, IOException, APIAccessException {
    	return this.drones.stream() // Get first result given input string, or throw exception if none found
    			.filter(drone -> Objects.equals(ID, drone.ID))
    			.findFirst()
    			.orElseThrow(() -> new APIAccessException(ID + " is not a valid drone ID", 400));
    }
    
    protected ArrayList<Drone> getDrones() { return this.drones; }
    
    protected boolean getStatus(Drone drone) throws IOException, InterruptedException { // Return drone power status (true = on | false = off)
        return controller.command(drone.getIPMICMD("chassis power status | grep 'on'")).succeeds();
    }

    protected boolean powerOn(Drone drone) throws IOException, InterruptedException { // Power on the drone
	    synchronized (drone) {
	    	if (this.getStatus(drone)) // If the drone is already on, no need to run commands
	            return true;
	
	        if (controller.command(drone.getIPMICMD("power on")).succeeds()) {
	            for (int i = 0; i < 12; i++) { // Check every 5 seconds to make sure the device powered on or timeout after a minute
	                Thread.sleep(5000);
	                if (this.getStatus(drone))
	                    return true;
	            }
	        }
	
	        return false;
		}    	
    }

    protected boolean powerOff(Drone drone) throws IOException, InterruptedException {
    	synchronized (drone) {
	        if (!this.getStatus(drone))
	            return true;
	
	        if (controller.command(drone.getIPMICMD("power off")).succeeds()) {
	            for (int i = 0; i < 12; i++) {
	                Thread.sleep(5000);
	                if (!this.getStatus(drone))
	                    return true;
	                }
	        }
	
	        return false;
    	}
    }
    
    protected boolean hasSnapshot(Drone drone) throws IOException, InterruptedException { // Check if drone is running an OS
        return controller.command("dmsetup table " + drone.name).succeeds();
    }

    protected boolean removeSnapshot(Drone drone) throws IOException, InterruptedException { // Remove OS from drone
    	synchronized (drone) {
	        if (!this.hasSnapshot(drone)) // If the drone already has no OS, no need to run
	            return true;
	
	        if (this.powerOff(drone)) {
	            controller.command("targetcli /backstores/block delete " + drone.name); // Delete iSCSI target
	            controller.command("dmsetup remove /dev/mapper/" + drone.name); // Delete mapped device
	            controller.command("losetup -d /dev/loop" + drone.loopNumber); // Delete loop device
	            controller.command("rm " + drone.COW); // Delete COW image
	            return true;
	        }
	
	        return false;
    	}
    }

    protected String getSnapshot(Drone drone) throws IOException, InterruptedException { // Get the name of the OS running on drone
        if (this.hasSnapshot(drone)) {
            return controller.command("dmsetup ls | grep $(dmsetup table " + drone.name + " | awk '{print $4}') | awk '{print $1}'").getOutput();
        }

        return null;
    }

    protected boolean setSnapshot(Drone drone, OS targetOS) throws IOException, InterruptedException, APIAccessException { // Set OS on drone
    	synchronized (drone) {
	        if (this.removeSnapshot(drone)) {
	            CommandResult getBlockSize = controller.command("blockdev --getsize " + targetOS.path);
	            if (!getBlockSize.succeeds()) {
	                throw new APIAccessException("Failed to set snapshot: Invalid image name given", 400);
	            }
	
	            int blockSize = Integer.parseInt(getBlockSize.getOutput());
	            controller.command("rm " + drone.COW); // Delete COW image to avoid conflicts
	            return controller.command("dd if=/dev/zero of=" + drone.COW + " bs=512 count=0 seek=" + blockSize).succeeds()
	                    && controller.command("losetup /dev/loop" + drone.loopNumber + " " + drone.COW).succeeds()
	                    && controller.command("echo \"0 " + blockSize + " snapshot " + targetOS.path + " /dev/loop" + drone.loopNumber + " p 64\" | dmsetup create " + drone.name).succeeds()
	                    && controller.command("targetcli /backstores/block create " + drone.name + " /dev/mapper/" + drone.name).succeeds()
	                    && controller.command("targetcli " + drone.IQN + "/tpg1/acls/" + drone.droneIQN + " create 0 /backstores/block/" + drone.name).succeeds()
	                    && this.powerOn(drone);
	        }
	
	        return false;
    	}
    } 
}
