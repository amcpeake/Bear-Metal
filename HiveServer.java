package bearmetal;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.service.CorsService;
import org.restlet.util.Series;

import java.util.Collections;
import java.util.HashSet;

/**
 * Restlet server configuration
 * Defines ports used, URI routing, and various HTTP(S) configurations
 * @author  Aidan McPeake
 */

public class HiveServer {
    private static final String ROOT_URI = "file:///var/www/html/"; // Base folder for file server
    private static final String KEYSTORE_PATH = "/BearMetal/API/hive.jks"; // Path to HTTPS keystore

    public static void main(String[] args) throws Exception {
        Component component = new Component();
        component.getClients().add(Protocol.FILE);
        component.getServers().add(Protocol.HTTP, 80);
        
        // HTTPS Configuration
        Server server = component.getServers().add(Protocol.HTTPS,  443);   
        Series<Parameter> parameters = server.getContext().getParameters();
        parameters.add("sslContextFactory", "org.restlet.engine.ssl.DefaultSslContextFactory");
        parameters.add("keyStorePath", KEYSTORE_PATH);
        parameters.add("keyStorePassword", "password");
        parameters.add("keyPassword", "password");
        parameters.add("keyStoreType", "JKS");
        
        // Cross Origin Resource Sharing (CORS) configuration
        CorsService cors = new CorsService(); 
        cors.setAllowedOrigins(new HashSet<>(Collections.singletonList("*")));
        cors.setAllowedCredentials(true);
        component.getServices().add(cors);

        // File server app definition
        Application fileserver = new Application() {
            @Override
            public Restlet createInboundRoot() {
            	return new Directory(getContext(), ROOT_URI);
            }
        };

        // API app definition
        Application api = new Application() {
            @Override
            public synchronized Restlet createInboundRoot() {
                Router r = new Router(getContext());
                r.attachDefault(HiveHandler.class);
                return r;
            }
        };

        component.getDefaultHost().attach("/files/", fileserver);
        component.getDefaultHost().attach("/api", api);
        
        
        try {
        	component.start();
        }
        catch (Exception e) {
        	System.out.println("Failed to initialize HTTPS server, reverting to HTTP");
        	component.getServers().remove(server);
        	component.start();
        }
    }
}





