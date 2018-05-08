package Raft;

import java.util.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * This class represents a Raft server that will be created, will either be a leader or a follower
 * depending on the argument parameters that are read when it is created.
 */
public class ServerMain {
    public static void main(String[] args) throws Exception {
        //ArrayList<LogEntry> logEntries = new ArrayList<>();
        LogEntryList logEntries = new LogEntryList();
        ArrayList<ServerInfo> secondariesMap = new ArrayList<>();

        String host = args[1].trim(), isLeader = args[5].trim();
        int port = Integer.valueOf(args[3]);
        PropertiesLoader properties = new PropertiesLoader();
        //System.out.println("Leader host+port = " + properties.getLeaderHost() + ":"
        //        + properties.getLeaderPort());

        Server server = new Server(port);
        ServletHandler handler = new ServletHandler();

        if (isLeader.equals("true")) {
            // This node will be the leader and will be responsible for registering followers,
            // replicating data, and other functions
            System.out.println("Starting up Raft leader with host:port = " + host + ":" + port);
        }

        handler.addServletWithMapping(new ServletHolder(new AppendEntryServlet(logEntries)), "/appendentry");
        handler.addServletWithMapping(new ServletHolder(new RegPrimaryServlet(secondariesMap, host, port)), "/register/*");
        handler.addServletWithMapping(new ServletHolder(new RecNewSecondary(secondariesMap)), "/newsecondary/*");
        //handler.addServletWithMapping(new ServletHolder(new ListServlet(events)), "/list");

        server.setHandler(handler);
        server.start();

        SecondaryFunctions secondary = new SecondaryFunctions(host, port, secondariesMap);
        secondary.checkSecondary(properties, isLeader);

        server.join();

    }
}
