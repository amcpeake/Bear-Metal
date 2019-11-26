package bearmetal;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Called by HiveServer each time a request is received
 * Request is accessed and parsed, and response is modified accordingly
 * @author  Aidan McPeake
 */

public class HiveHandler extends ServerResource {
    private static final Logger log = LoggerFactory.getLogger(HiveHandler.class);
    private static final DroneController droneController = DroneController.getInstance();
    
    // Specifies accepted HTTP methods
    @Get
    @Post
    public void accept(){}

    // Class entrypoint; Parses user JSON and passes it to handleRequest()
    @Override
    public void doInit() {
        JSONObject requestJSON = new JSONObject();
        
        try {
        	if (getRequestEntity().getMediaType() != null && getRequestEntity().getMediaType().toString().equals("application/json")) { // If Content-Type header is JSON, get input as JSON
            	// JSON Format: {"action": str, "drone": str|int, "OS": str}
            	requestJSON = new JSONObject(getRequest().getEntityAsText());
        	}

        	else {
            	requestJSON.put("action", ""); // Provide default action value
        	}

        	handleRequest(requestJSON);
        }
        
        catch(JSONException e) { // Invalid JSON provided by user
        	log.error(e.getClass().toString(), e);
        	this.getResponse().setStatus(new Status(400));
        }
    }

    // Take parsed JSON and send it to the right place
    private void handleRequest(JSONObject requestJSON) throws JSONException { 
        int statusCode = 200;
        JSONObject responseJSON = new JSONObject();
        Drone drone = null;
        OS os = null;

        try {
            if (requestJSON.has("drone")) { // If a drone value is present, attempt to instantiate
            	if (requestJSON.get("drone") instanceof String) {
            		if (StringUtils.isNumeric(requestJSON.getString("drone")))
            			drone = droneController.get(Integer.parseInt(requestJSON.getString("drone")));
            		else
            			drone = droneController.get(requestJSON.getString("drone"));
            	}
            	else if (requestJSON.get("drone") instanceof Integer)
            		drone = droneController.get(requestJSON.getInt("drone"));
            }
            
            if (requestJSON.has("OS") && requestJSON.get("OS") instanceof String) { // If OS value is present, attempt to instantiate
                os = OS.get(requestJSON.getString("OS"));
            }

            switch (requestJSON.getString("action")) {
                case "": // Returns error
                    throw new APIAccessException("An action is required", 400);

                case "poweron": // Powers on drone
                    if (droneController.powerOn(drone)) {
                        responseJSON.put("msg", drone.name + " powered on");
                        break;
                    }

                    throw new APIAccessException(drone.name + " failed to power on", 500);

                case "poweroff": // Power off drone
                    if (droneController.powerOff(drone)) {
                        responseJSON.put("msg", drone.name + " powered off");
                        break;
                    }

                    throw new APIAccessException(drone.name + " failed to power off", 500);

                case "getsnapshot": // Check if drone is running an OS
                    if (droneController.hasSnapshot(drone)) {
                        responseJSON.put("msg", drone.name + " is running " + droneController.getSnapshot(drone));
                        responseJSON.put("snapshot", true);
                        break;
                    }

                    responseJSON.put("msg", drone.name + " does not have a running OS");
                    responseJSON.put("snapshot", false);
                    break;

                case "setsnapshot": // Deploy an OS to a drone
                    boolean force = false;

                    if (requestJSON.has("force") && requestJSON.get("force") instanceof Boolean) {
                        force = requestJSON.getBoolean("force");
                    }

                    if (!force && (droneController.hasSnapshot(drone) || droneController.getStatus(drone))) {
                        throw new APIAccessException("drone is already running. Either unregister it first, or force it.", 400);
                    }

                    if (droneController.setSnapshot(drone, os)) {
                        responseJSON.put("msg", drone.name + " is successfully running " + os.name);
                        break;
                    }

                    throw new APIAccessException("Failed to set OS on " + drone.name, 500);

                case "getdrones": // Build a list of available drones, their status, OS, IPMI info
                    List<Drone> drones;
                    
                    if (drone != null) {
                    	drones = Arrays.asList(drone);
                    }
                    else {
                    	drones = droneController.getDrones();
                    }
                    
                    responseJSON.put("drones", new JSONArray(drones.stream().map(d -> 
								{
									try {
										return new JSONObject() {{
												put("name", d.name);
										        put("status", droneController.getStatus(d) ? "on" : "off");
										        put("OS", (droneController.getSnapshot(d)) == null ? "" : droneController.getSnapshot(d));
										        put("registree", d.getRegistree() == null ? "" : d.getRegistree());
										        put("type", d.type);
										        put("IP", d.pubIP);
										        put("ID", d.ID);
										        put("IPMI", new JSONObject() {{
										                put("user", d.username);
										                put("password", d.password);
										        }});
										}};
									} catch (Exception e) {}
									return new JSONObject();
								}
                    ).collect(Collectors.toList())));
                    
                    break;

                case "getos": // Gather a list of available OS
                    if (requestJSON.has("filter")) {
                    	responseJSON.put("OS", OS.findOS(requestJSON.getString("filter")));
                    	break;
                    }

                    responseJSON.put("OS", OS.findOS(""));
                    break;

                case "register": // Assign a user's name to a drone
                    if (drone.getRegistree() == null) {
                        drone.setRegistree(requestJSON.get("registree").toString());
                        responseJSON.put("msg", "Successfully registered " + drone.name + " to " + requestJSON.get("registree"));
                        break;
                    }

                    throw new APIAccessException("drone is already registered to " + drone.getRegistree(), 500);


                case "unregister": // Reset drone to default configuration and remove registration
                    if (droneController.removeSnapshot(drone)) {
                        drone.setRegistree(null);
                        responseJSON.put("msg", "Successfully unregistered " + drone.name);
                        break;
                    }

                    throw new APIAccessException("Failed to unregister " + drone.name, 500);

                default:
                    throw new APIAccessException(requestJSON.get("action").toString() + " is not a valid action", 400);
            }
        }

        catch(APIAccessException e) { // Custom exception, thrown by HiveHandler/Drone/OS
            responseJSON.put("msg", e.getMessage());
            statusCode = e.getExitCode();

            log.error(e.getClass().toString(), e);
        }
        
        catch(NullPointerException | JSONException e) { // Missing or invalid parameter; User error
        	responseJSON.put("msg", "Missing/Invalid parameter given"); 
            statusCode = 400;

            log.error(e.getClass().toString(), e);
        }

        catch(IOException | InterruptedException e){ // Thrown by CLIProcess when a shell command fails; Server error
            responseJSON.put("msg", "System call caused error; Please try again later");
            statusCode = 500;

            log.error(e.getClass().toString(), e);
        }

        catch (Exception e) { // Unknown/unhandled error
            responseJSON.put("msg", "Unhandled error detected; May cause unexpected behaviour");
            statusCode = 500;

            log.error(e.getClass().toString(), e);
        }

        this.getResponse().setStatus(new Status(statusCode));
        this.getResponse().setEntity(new JsonRepresentation(responseJSON));
    }
}
