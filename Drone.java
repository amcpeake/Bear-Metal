package bearmetal;

import java.util.LinkedHashMap;

/**
 * Contains information about drones gathered
 * from yaml file.
 * @author  Aidan McPeake
 */

public class Drone {
    protected final String name;
    protected final int ID;
    protected final String type; 
    protected final String pubIP;
    protected final String IPMI_IP;
    protected final String username;
    protected final String password;
    private String registree = null;
    
    private static final String BASEIQN = "iqn.2003-01.org.linux-iscsi.bearmetal";
    private static final String BASECOW = "/mnt/images/cow/";
    private static final String BASEDRONEIQN = "iqn.2018-01.org.ipxe";
    private static final int BASENUMBER = 10;
    protected final String IQN; // iSCSI Target String
    protected final String droneIQN; // iSCSI Target ACL String
    protected final String COW; // Drone Copy-on-Write (COW) file path 
    protected final int loopNumber; // Loop device ID
    
    protected Drone(LinkedHashMap<String, Object> drone) {
    	this.name = (String) drone.get("name");
    	this.ID = (int) drone.get("ID");
    	this.type = (String) drone.get("type");
    	this.pubIP = (String) drone.get("pubIP");
    	this.IPMI_IP = (String) drone.get("IPMI_IP");
    	this.username = (String) drone.get("username");
    	this.password = (String) drone.get("password");
    	
    	this.IQN = "/iscsi/" + BASEIQN + ":" + this.name;
        this.COW = BASECOW + this.name;
        this.droneIQN = BASEDRONEIQN + ":" + this.name;
        this.loopNumber = BASENUMBER + this.ID;
    }

    protected String getRegistree() { return this.registree; }
    protected void setRegistree(String registree) { this.registree = registree; }
    
    protected String getIPMICMD(String command) { // Return base IPMI command syntax: ipmitool -H <IP> -U <user> -P <pass> <command>
        return "ipmitool -H " + this.IPMI_IP + " -U " + this.username + " -P " + this.password + " " + command;
    }
}
