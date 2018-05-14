package Raft;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * This servlet will process AppendEntry RPC's. It will have two main responsibilities which are
 * to receive RPC's with no data for heartbeat messages and receive RPC's with data to replicate
 * to its own log. This class and its doPost will always be processed by the followers.
 */
public class AppendEntryServlet extends HttpServlet{
    private LogEntryList logEntries;
    private ArrayList<ServerInfo> secondariesMap;
    private SecondaryFunctions secondary;
    private int majority;

    public AppendEntryServlet(LogEntryList logs, SecondaryFunctions s, ArrayList<ServerInfo> sMap){
        logEntries = logs;
        secondary = s;
        secondariesMap = sMap;
        majority = 3;
    }

    /**
     * Check if the incoming message has any data, if it does then replicate and send a 200
     * response back. Else simply send a 200 response back.
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        String jsonString = getJsonString(request);
        //System.out.println("Inside AppendEntryServlet, jsonString = " + jsonString);

        JSONParser parser = new JSONParser();
        JSONObject object = null;
        JSONObject object2 = null;
        int lastTerm = 0, lastIndex = 0;

        try {
            object = (JSONObject) parser.parse(jsonString);
            object2 = (JSONObject) object.get("data");
            lastTerm = Integer.valueOf(object.get("lastTerm").toString());
            lastIndex = Integer.valueOf(object.get("lastIndex").toString());
        }
        catch (ParseException e){
            e.printStackTrace();
        }

        //System.out.println("data object = " + object2.toString());
        System.out.println("last term and last index = " + lastTerm + ", " + lastIndex);

        if(object2.isEmpty()){
            //System.out.println("This is a heartbeat");
            response.setStatus(HttpServletResponse.SC_OK);
            boolean timerSet = secondary.getTimerSet();

            if(!timerSet){
                // create thread and start timer with randomized election timeout (150-300 ms)
                secondary.setTimerSet(true);
                try {
                    secondary.startTimer();
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            else{ // reset timer in thread using boolean variable
                secondary.resetTimer();
            }

            // if this follower's current index is less than the index of the leader, then
            // increment the index of the follower and commit that entry
            int currentIndex = secondary.getIndex();
            if(currentIndex < lastIndex){
                // lastIndex should be 0 if nothing is on log, log index starts at 1
                secondary.incrementIndex();

                // commit the entry to persistent data store
            }
        }
        else{
            System.out.println("not a heartbeat, trying to append data to log");
            boolean isLeader = secondary.getLeader();

            if(isLeader){
                // add to log entry, replicate to followers, return 200 when majority of followers
                // added new entry, then commit
                response.setStatus(HttpServletResponse.SC_OK);
                int count = 0;
                int newTerm = secondary.getTerm() + 1;
                int newIndex = secondary.getIndex() + 1;
                LogEntry entry = new LogEntry(newTerm, newIndex, object2);
                logEntries.addToList(entry);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("lastTerm", newTerm);
                jsonObject.put("lastIndex", newIndex);
                jsonObject.put("data", object2);

                for(ServerInfo server: secondariesMap){
                    String host = server.getHost();
                    int port = server.getPort();

                    boolean sent = sendAppendEntryRpc(host, port, jsonObject);
                    if(sent){
                        count++;
                    }
                }

                if(count >= 3){
                    // commit log entry

                    secondary.incrementIndex();
                }
                else{
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }

            }
            else{
                // secondary should just add to log entry and send 200 response back but not commit
                LogEntry entry = new LogEntry(lastTerm, lastIndex, object2);
                logEntries.addToList(entry);

                response.setStatus(HttpServletResponse.SC_OK);
            }

        }

        /*
        if(jsonString.equals("") || jsonString == null){
            // if there is no data that is sent then this is a heartbeat, send 200 response back
            response.setStatus(HttpServletResponse.SC_OK);
            boolean timerSet = secondary.getTimerSet();

            if(!timerSet){
                // create thread and start timer with randomized election timeout (150-300 ms)
                secondary.setTimerSet(true);
                try {
                    secondary.startTimer();
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            else{ // reset timer in thread using boolean variable
                secondary.resetTimer();
            }

        }
        else{ // if there is data that is sent, then replicate the log
            // If it is a leader, increment term

            if(secondary.getLeader()){ // If a leader received appendEntryRpc with data

            }
            else{ // if a follower received an appendEntryRpc with data

            }

            //response.setStatus(HttpServletResponse.SC_OK);
        }
        */

    }

    /**
     * This class reads the Json input that is sent with a post request and returns a String
     * version of it.
     * @param request
     * @return
     * @throws IOException
     */
    public String getJsonString(HttpServletRequest request) throws IOException{
        BufferedReader jsonInput = request.getReader();
        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = jsonInput.readLine()) != null) {
            builder.append(line);
        }

        return builder.toString();
    }

    public boolean sendAppendEntryRpc(String secondaryHost, int secondaryPort, JSONObject object){
        String url = "http://" + secondaryHost + ":" + secondaryPort + "/appendentry";
        int statusCode = 0;

        try{
            URL objUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) objUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-type", "application/json");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(object.toString());
            writer.flush();

            statusCode = connection.getResponseCode();
        }
        catch(Exception e){
            System.out.println("secondary with port " + secondaryPort + " is offline");
            statusCode = 400;
        }

        return (statusCode == 200);
    }

}
