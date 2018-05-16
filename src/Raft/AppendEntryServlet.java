package Raft;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * This servlet will process AppendEntry RPC's. It will have two main responsibilities which are
 * to receive RPC's with no data for heartbeat messages and receive RPC's with data to replicate
 * to its own log. This class and its doPost will always be processed by the followers.
 */
public class AppendEntryServlet extends HttpServlet{
    private int port;
    private JSONArray logEntries;
    private ArrayList<ServerInfo> secondariesMap;
    private SecondaryFunctions secondary;
    private int majority;

    public AppendEntryServlet(int p, JSONArray logs, SecondaryFunctions s, ArrayList<ServerInfo> sMap){
        port = p;
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
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (object2.isEmpty()) { // this is processing a heartbeat message
            response.setStatus(HttpServletResponse.SC_OK);
            boolean timerSet = secondary.getTimerSet();
            boolean isCandidate = secondary.getCandidate();
            boolean secondElection = secondary.getSecondElection();

            if(isCandidate){ // restore this secondaries voting rights
                secondary.setCandidate(false);
            }

            if(secondElection){ // if secondElection = true, there was an election timeout but new leader got elected
                secondary.setSecondElection(false);
            }

            if (!timerSet) {
                // create thread and start timer with randomized election timeout (150-300 ms)
                secondary.setTimerSet(true);
                try {
                    secondary.startTimer();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else { // reset timer in thread using boolean variable
                secondary.resetTimer();
            }

            // if this follower's current index is less than the index of the leader, then
            // increment the index of the follower and commit that entry
            int currentIndex = secondary.getIndex();
            if (currentIndex < lastIndex) {
                // lastIndex should be 0 if nothing is on log, log index starts at 1
                secondary.incrementIndex();

                // commit the entry to persistent data store
                System.out.println("Received updated index in heartbeat, will commit entry to data store ");
                String filename = port + ".json";
                commitEntryToFile(filename);
            }
        } else {
            System.out.println("jsonString = " + jsonString);
            System.out.println("not a heartbeat, trying to append data to log");
            //System.out.println("(ignore for leader)last term and last index = " + lastTerm + ", " + lastIndex);
            boolean isLeader = secondary.getLeader();

            if (isLeader) {
                // add to local log entry, replicate to followers, return 200 when majority of followers
                // added new entry, then commit
                response.setStatus(HttpServletResponse.SC_OK);
                int count = 0;
                int newTerm = secondary.getTerm();
                int newIndex = secondary.getIndex() + 1;

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("lastTerm", newTerm);
                jsonObject.put("lastIndex", newIndex);
                jsonObject.put("data", object2);
                logEntries.add(jsonObject);

                System.out.println("Leader added new entry to its own log, now sending to followers");
                for (ServerInfo server : secondariesMap) {
                    String host = server.getHost();
                    int port = server.getPort();

                    boolean sent = sendAppendEntryRpc(host, port, jsonObject);
                    System.out.println("Sent status to follower with port " + port + " = " + sent);
                    if (sent) {
                        count++;
                    }
                }

                System.out.println("count total = " + count);
                if (count >= majority) { // leader replicated to a majority of the followers
                    // commit log entry and increment Index
                    System.out.println("Leader replicated to a majority of followers, will commit and increment index");
                    String filename = port + ".json";
                    System.out.println("filename = " + filename);
                    commitEntryToFile(filename);

                    secondary.incrementIndex();
                } else {
                    System.out.println("Leader didn't replicate to a majority of its followers");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }

            } else {
                // secondary should just add to log entry and send 200 response back but not commit
                // it should also check index and term to see if it matches, if it doesn't get info
                // from leader

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("lastTerm", lastTerm);
                jsonObject.put("lastIndex", lastIndex);
                jsonObject.put("data", object2);
                logEntries.add(jsonObject);

                System.out.println("Secondary added data from leader to log entry list");
                response.setStatus(HttpServletResponse.SC_OK);

            }

        } // end of else (for if it is not heartbeat)
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

    public void commitEntryToFile(String filename) throws IOException{
        // read the json array from json file
        Path file = Paths.get(filename);
        String input = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        JSONObject obj = (JSONObject) JSONValue.parse(input);

        // replace old storage with updated storage
        obj.put("storage", logEntries);

        String output = JSONValue.toJSONString(obj);
        Files.write(file, output.getBytes(StandardCharsets.UTF_8));
    }

    public JSONArray requestInfoFromLeader(String host, int port) throws  Exception {
        String url = "http://" + host + ":" + port + "/loginfo";

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        JSONObject object = new JSONObject();
        object.put("loginfo", 1);

        OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
        writer.write(object.toString());
        writer.flush();

        int statusCode = con.getResponseCode();
        System.out.println("Status code for getting log info = " + statusCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String line;
        StringBuilder builder = new StringBuilder();

        while ((line = in.readLine()) != null) {
            builder.append(line);
        }

        String jsonString = builder.toString();
        System.out.println("Log info json = " + jsonString);

        JSONParser parser = new JSONParser();
        JSONObject jsonObj = (JSONObject) parser.parse(jsonString);
        JSONArray log = (JSONArray) jsonObj.get("storage");

        return log;
    }

}
