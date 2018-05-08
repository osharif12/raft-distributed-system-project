package Raft;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a servlet for a follower node that processes a GET request sent by another follower
 * node that just started up and connected with the leader. It adds the new follower to its
 * list of followers.
 */
public class RecNewSecondary extends HttpServlet {
    private ArrayList<ServerInfo> secondaryMap;

    public RecNewSecondary(ArrayList<ServerInfo> map){
        secondaryMap = map;
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

        if (m.matches()) {
            secondaryHost = m.group(1);
            secondaryPort = Integer.valueOf(m.group(2));
        }


        boolean exists = false;
        for(ServerInfo temp1: secondaryMap){
            if(temp1.getPort() == secondaryPort){
                exists = true;
            }
        }

        if(!exists) {
            ServerInfo temp = new ServerInfo(secondaryPort, secondaryHost);
            secondaryMap.add(temp);
        }

        //log.debug("Discovered new secondary service with port " + secondaryPort);
        //log.debug("See updated list of secondaries below");
        System.out.println("See updated list of followers below");
        for(ServerInfo e: secondaryMap){
            //log.debug("Secondary with port " + e.getPort());
            System.out.println("Follower with port " + e.getPort());
        }
    }

}
