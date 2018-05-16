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
import java.io.PrintWriter;

public class LeaderInfoServlet extends HttpServlet{

    public LeaderInfoServlet(){

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = response.getWriter();

        PropertiesLoader propertiesLoader = new PropertiesLoader();
        String host = propertiesLoader.getLeaderHost();
        //int port = Integer.valueOf(propertiesLoader.getLeaderPort());
        String port = propertiesLoader.getLeaderPort();

        JSONObject object = new JSONObject();
        object.put("leaderhost", host);
        object.put("leaderport", port);

        out.println(object);



        //BufferedReader jsonInput = request.getReader();
        //StringBuilder builder = new StringBuilder();
        //String line;

        /*
        while ((line = jsonInput.readLine()) != null) {
            builder.append(line);
        }

        String jsonString = builder.toString();

        JSONParser parser = new JSONParser();
        JSONObject object;

        int userid = 0, numtickets = 0;
        String eventname = "";

        try {
            object = (JSONObject) parser.parse(jsonString);
            userid = (int)Long.parseLong((object.get("userid").toString()));
            eventname = object.get("eventname").toString();
            numtickets = (int)Long.parseLong((object.get("numtickets").toString()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if(numtickets < 0 || eventname.equals("")){ // check if number of tickets is below 0
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        else {
            // checks if userid is valid
            int statusCode = checkUserId(userid);

            if (statusCode == 200) { // set status to 200, create a new event, and add it to list
                response.setStatus(HttpServletResponse.SC_OK);

                EventData event = new EventData(userid, eventname, numtickets);

                // output the json with eventid
                int eventId = event.getEventId();
                JSONObject object1 = new JSONObject();
                object1.put("eventid", eventId);
                out.println(object1);

                eventList.addToList(event);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    */

    }
}
