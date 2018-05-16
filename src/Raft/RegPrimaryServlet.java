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
 * This class will instruct the Leader to process the GET request sent by the follower upon starting and
 * will send leader list of followers to followers. It will also take follower
 * host and port and add info to primary database of secondaries.
 */
public class RegPrimaryServlet extends HttpServlet {
    private SecondaryFunctions secondary;
    private ArrayList<ServerInfo> secondaryMap;
    private String leaderHost;
    private int leaderPort;
    private JSONArray logEntries;

    public RegPrimaryServlet(SecondaryFunctions sec, ArrayList<ServerInfo> smap, String host,
                             int port, JSONArray logs){
        secondary = sec;
        secondaryMap = smap;
        leaderHost = host;
        leaderPort = port;
        logEntries = logs;
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

        JSONObject finalObj = new JSONObject();
        finalObj.put("secondaries", jsonArray);
        finalObj.put("storage", logEntries);
        int term = secondary.getTerm();
        finalObj.put("term", term);

        PrintWriter writer = response.getWriter();
        writer.println(finalObj.toString());
        writer.flush();
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
        PrimaryFunctions primary = new PrimaryFunctions(secondary, secondaryMap, primaryHost, primaryPort);
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
