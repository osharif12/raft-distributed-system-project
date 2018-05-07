package Raft;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class will instruct the Primary to process the GET request sent by the secondary upon starting and
 * will send primary list of events, secondaries, and front ends to secondary. It will also take secondary
 * host and port and add info to primary database of secondaries. It should also send all the secondaries
 * in its list data about the new secondary.
 */
public class RegPrimaryServlet extends HttpServlet {
    private ArrayList<ServerInfo> secondaryMap;
    private String leaderHost;
    private int leaderPort;

    public RegPrimaryServlet(ArrayList<ServerInfo> smap, String host, int port){
        secondaryMap = smap;
        leaderHost = host;
        leaderPort = port;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);

        String pathInfo = request.getPathInfo();
        String secondaryHost = "";
        int secondaryPort = 0;
        String regex = "\\/host=(.*?)port=([0-9]*)";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(pathInfo);
        if(m.matches()){
            secondaryHost = m.group(1);
            secondaryPort = Integer.valueOf(m.group(2));
        }

        // create a JSONArray for all secondary nodes in secondaryMap
        JSONArray jsonArray = new JSONArray();
        for(ServerInfo temp: secondaryMap){
            int port = temp.getPort();
            String host = temp.getHost();
            JSONObject object = createJSON(host, port);
            jsonArray.add(object);
        }

        // add secondary server info to primary secondaryMap
        ServerInfo temp1 = new ServerInfo(secondaryPort, secondaryHost);
        if(!secondaryExists(secondaryPort)){
            secondaryMap.add(temp1);
            startHeartbeat(leaderHost, leaderPort, secondaryHost, secondaryPort);
            System.out.println("added new secondary to secondaryMap of primary");
        }
        //secondaryMap.add(temp1);

        JSONObject finalObj = new JSONObject();
        finalObj.put("secondaries", jsonArray);

        PrintWriter writer = response.getWriter();
        writer.println(finalObj.toString());
        writer.flush();

        //startHeartbeat(leaderHost, leaderPort, secondaryHost, secondaryPort);

        /*
        String pathInfo = request.getPathInfo();
        String secondaryHost = "";
        int secondaryPort = 0;
        String regex = "\\/host=(.*?)port=([0-9]*)";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(pathInfo);
        if(m.matches()){
            secondaryHost = m.group(1);
            secondaryPort = Integer.valueOf(m.group(2));
        }

        log.debug("Discovered new secondary with port = " + secondaryPort);
        ArrayList<EventData> list = eventList.getList();
        JSONArray jsonArray = getEventJsonArray(list);

        // create a JSONArray for all secondary nodes in secondaryMap
        JSONArray jsonArray1 = new JSONArray();
        for(ServerInfo temp: secondaryMap){
            int port = temp.getPort();
            String host = temp.getHost();
            JSONObject object = createJSON2(host, port);
            jsonArray1.add(object);
        }

        // add secondary server info to primary secondaryMap
        ServerInfo temp1 = new ServerInfo(secondaryPort, secondaryHost);
        secondaryMap.add(temp1);

        // create a JSONArray for all front ends
        JSONArray jsonArray2 = new JSONArray();
        for(ServerInfo temp: frontEndMap){
            int port = temp.getPort();
            String host = temp.getHost();
            JSONObject object = createJSON2(host, port);
            jsonArray2.add(object);
        }

        JSONObject finalObj = new JSONObject();
        finalObj.put("events", jsonArray);
        finalObj.put("secondaries", jsonArray1);
        finalObj.put("frontends", jsonArray2);

        PrintWriter writer = response.getWriter();
        writer.println(finalObj.toString());
        writer.flush();

        startHeartbeat(primaryHost, primaryPort, secondaryHost, secondaryPort);
        */
    }

    public JSONObject createJSON(String host, int port){
        JSONObject object = new JSONObject();

        object.put("port", port);
        object.put("host", host);

        return object;
    }

    /**
     * This method creates instance of PrimaryFunctions class and starts heartbeats to secondary.
     * @param secondaryHost
     * @param secondaryPort
     */
    public void startHeartbeat(String primaryHost, int primaryPort, String secondaryHost, int secondaryPort){
        PrimaryFunctions primary = new PrimaryFunctions(secondaryMap, primaryHost, primaryPort);
        primary.sendHeartbeatsToSecondary(secondaryHost, secondaryPort);
    }

    public boolean secondaryExists(int port){
        boolean ret = false;

        for(ServerInfo temp: secondaryMap){
            int tPort = temp.getPort();
            if(port == tPort){
                ret = true;
            }
        }

        return ret;
    }

}
