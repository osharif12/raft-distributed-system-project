import java.util.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class represents a Raft server that will be created, will either be a leader or a follower
 * depending on the argument parameters that are read when it is created.
 */
public class ServerMain {
    public static void main(String[] args) throws Exception {

        String host = args[1].trim(), isPrimary = args[5].trim();
        int port = Integer.valueOf(args[3]);

        Server server = new Server(port);
        ServletHandler handler = new ServletHandler();

        if (isPrimary.equals("true")) {
            // This node will be the leader and will be responsible for registering followers,
            // replicating data, and other functions


        }

        //handler.addServletWithMapping(new ServletHolder(new EventServlet(events)), "/*");
        //handler.addServletWithMapping(new ServletHolder(new ListServlet(events)), "/list");

        server.setHandler(handler);
        server.start();
        server.join();


    }
}
