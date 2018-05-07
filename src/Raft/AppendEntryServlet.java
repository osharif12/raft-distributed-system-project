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
 * to its own log. This class and its doPost will always be processed by the secondaries.
 */
public class AppendEntryServlet extends HttpServlet{
    private LogEntryList logEntries;

    public AppendEntryServlet(LogEntryList logs){
        logEntries = logs;
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

        if(jsonString.isEmpty()){ // if there is no data that is sent, send 200 response back
            response.setStatus(HttpServletResponse.SC_OK);
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





        /*
        response.setContentType("application/json");
        String path = request.getPathInfo();
        path = path.substring(1);
        int urlEventId = Integer.parseInt(path);

        String jsonString = getJsonString(request);
        JSONParser parser = new JSONParser();

        int userid = 0, eventid = 0, tickets = 0, ticketsAvail = 0, ticketsPurch = 0;
        String isPrimary = "", eventname = "";
        JSONParser parser1 = new JSONParser();
        JSONObject object1;

        try{
            object1 = (JSONObject) parser1.parse(jsonString);
            isPrimary = object1.get("isprimary").toString();
        }
        catch (ParseException e){
            e.printStackTrace();
        }

        if(isPrimary.equals("true")){
            // This function when called will check if event exists and if tickets can be purchased
            primaryAction(parser, jsonString, userid, eventid, urlEventId, tickets, response);
        }
        else{ // if this is a secondary event service, just replicate
            replicateSecondary(jsonString);
            response.setStatus(HttpServletResponse.SC_OK);
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



}
