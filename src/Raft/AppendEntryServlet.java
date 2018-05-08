package Raft;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * This servlet will process AppendEntry RPC's. It will have two main responsibilities which are
 * to receive RPC's with no data for heartbeat messages and receive RPC's with data to replicate
 * to its own log. This class and its doPost will always be processed by the followers.
 */
public class AppendEntryServlet extends HttpServlet{
    private LogEntryList logEntries;
    private static int term;
    private SecondaryFunctions secondary;
    private static boolean timerSet;

    public AppendEntryServlet(LogEntryList logs, int t, SecondaryFunctions s, boolean timer){
        logEntries = logs;
        term = t;
        secondary = s;
        timerSet = timer;
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

        if(jsonString.equals("") || jsonString == null){ // if there is no data that is sent, send 200 response back
            response.setStatus(HttpServletResponse.SC_OK);

            if(!timerSet){
                // create thread and start timer with randomized election timeout (150-300 ms)
                timerSet = true;
                secondary.startTimer();
            }
            else{ // reset timer in thread using boolean variable
                secondary.resetTimer();
            }

        }
        else{ // if there is data that is sent, then replicate the log
            JSONParser parser = new JSONParser();
            JSONObject object;
            int term, index, data;

            try{
                object = (JSONObject) parser.parse(jsonString);
                term = (int)Long.parseLong(object.get("term").toString());
                index = (int)Long.parseLong(object.get("index").toString());
                data = (int)Long.parseLong(object.get("data").toString());
            }
            catch (Exception e){
                e.printStackTrace();
            }

            response.setStatus(HttpServletResponse.SC_OK);
        }

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


}
