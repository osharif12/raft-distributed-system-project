package Raft;

import org.json.simple.JSONObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This servlet is used mainly by the ReplicationRequests class to get the leadership port and host
 * transferred to the client in the form of a json object.
 */
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
        String port = propertiesLoader.getLeaderPort();

        JSONObject object = new JSONObject();
        object.put("leaderhost", host);
        object.put("leaderport", port);

        out.println(object);

    }
}
