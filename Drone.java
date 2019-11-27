package bearmetal;

import java.util.LinkedHashMap;

/**
 * Contains information about drones gathered
 * from yaml file.
 * @author  Aidan McPeake
 */

public class Drone {
    private final String name;
    private final int ID;
    private final String type; 
    private final String frontIP; // Front-end IP address (Drone access)
    private final String backIP; // Back-end IP address (IPMI Control)
    private final String username;
    private final String password;
    private String registree = null;
    
    private static final String BASEIQN = "iqn.2003-01.org.linux-iscsi.bearmetal";
    private static final String BASECOW = "/mnt/images/cow/";
    private static final String BASEACL = "iqn.2018-01.org.ipxe";
    private static final int BASENUMBER = 10;
    private final String IQN; // iSCSI Target String
    private final String ACL; // iSCSI Target ACL String
    private final String COW; // Drone Copy-on-Write (COW) file path 
    private final int loopNumber; // Loop device ID
    
    protected Drone(LinkedHashMap<String, Object> drone) {
    	this.name = (String) drone.get("name");
    	this.ID = (int) drone.get("ID");
    	this.type = (String) drone.get("type");
    	this.frontIP = (String) drone.get("frontIP");
    	this.backIP = (String) drone.get("backIP");
    	this.username = (String) drone.get("username");
    	this.password = (String) drone.get("password");
    	
    	this.IQN = "/iscsi/" + BASEIQN + ":" + this.name;
        this.COW = BASECOW + this.name;
        this.ACL = BASEACL + ":" + this.name;
        this.loopNumber = BASENUMBER + this.ID;
    }

    protected String getName() { return this.name; }
    protected int getID() { return this.ID; }
    protected String getType() { return this.type; }
    protected String getFrontIP() { return this.frontIP; }
    protected String getBackIP() { return this.backIP; }
    protected String getUsername() { return this.username; }
    protected String getPassword() { return this.password; }
    protected String getRegistree() { return this.registree; }
    protected void setRegistree(String registree) { this.registree = registree; }
    protected String getIQN() { return this.IQN; }
    protected String getACL() { return this.ACL; }
    protected String getCOW() { return this.COW; }
    protected int getLoop() { return this.loopNumber; }
    
    protected String getIPMICMD(String command) { // Return base IPMI command syntax: ipmitool -H <IP> -U <user> -P <pass> <command>
        return "ipmitool -H " + this.backIP + " -U " + this.username + " -P " + this.password + " " + command;
    }
}
