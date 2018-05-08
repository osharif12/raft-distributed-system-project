package Raft;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestVoteServlet extends HttpServlet{
    private int term;
    private int vote;

    public RequestVoteServlet(int term1, int vote1){
        term = term1;
        vote = vote1;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String pathInfo = request.getPathInfo();
        int term2 = 0;
        String regex = "\\/term=([0-9]*)";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(pathInfo);
        if(m.matches()){
            term2 = Integer.valueOf(m.group(1));
        }

        System.out.println("Inside RequestVoteServlet, term read = " + term2);

        if(term < term2){ // increment term, set vote = 1,
            term++;
            vote = 1;
            vote--;
            response.setStatus(HttpServletResponse.SC_OK);
            System.out.println("Gave its vote to server requesting it");
        }
        else{ // terms should not be same or this term should not be greater than incoming term
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

    }


}
