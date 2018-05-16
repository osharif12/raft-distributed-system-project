package Raft;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestVoteServlet extends HttpServlet{
    private SecondaryFunctions secondary;

    public RequestVoteServlet(SecondaryFunctions secondary1){
        secondary = secondary1;
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

        // Only if the term of this service is less than incoming term, you should give vote,
        // if they are equal or greater then you should not give vote
        int term = secondary.getTerm();
        int vote = 0;
        boolean isCandidate = secondary.getCandidate(); // if isCandidate, cannot vote

        System.out.println("Inside RequestVoteServlet, term read = " + term2);
        System.out.println("This current term for this service = " + term);

        if(term < term2 && !isCandidate){ // increment term, set vote = 1,
            secondary.setCandidate(true); // set candidate to true so cannot vote for anyone else this term
            secondary.incrementTerm();
            vote = 1;
            vote--;
            response.setStatus(HttpServletResponse.SC_OK);
            System.out.println("Gave its vote to server requesting it");
            System.out.println("Updated term for this service is " + secondary.getTerm());
        }
        else{ // terms should not be same or this term should not be greater than incoming term
            System.out.println("Did not vote for server trying to be leader");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

    }


}
