package Raft;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

public class RequestVoteServlet extends HttpServlet{
    private LogEntryList logEntries;
    private static int term;
    private SecondaryFunctions secondary;
    private static boolean timerSet;

    public RequestVoteServlet(){

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    }


}
