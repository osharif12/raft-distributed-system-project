package Raft;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

public class Election {
    private String host;
    private int port;
    private ArrayList<ServerInfo> secondaryMap;
    private int term;
    private int majority;

    public Election(String host1, int port1, ArrayList<ServerInfo> sMap, int term1) {
        host = host1;
        port = port1;
        secondaryMap = sMap;
        term = term1;
        term++; // increment term

        majority = sMap.size() + 1; // all the servers that are eligible to become leader
        majority = (majority / 2) + 1; // number that consitutes a majority of all the eligible leaders
    }

    public boolean electLeader(){
        boolean ret = false, gotVote = false;
        int count = 1; // candidate will vote for itself
        System.out.println("This node is starting an election");
        System.out.println("This node became a candidate and incremented its term to " + term);

        for(ServerInfo temp: secondaryMap){ // send out the RequestVoteRpc's to all other followers
            String host = temp.getHost();
            int port = temp.getPort();

            gotVote = sendRequestVoteRpc(host, port);

            if(gotVote){ // if you got a vote, increment the count
                count++;
                System.out.println("Got a vote");
            }

        }

        if(count >= majority){ // if you get a majority of votes, new leader was elected,
            // send heartbeat messages to other nodes notifying them you are new leader, update
            // the properties file
            ret = true;
            System.out.println("This node was elected leader with " + count + " votes");

            try {
                changePrimaryConfig(host, port);
            }
            catch (Exception e){
                e.printStackTrace();
            }

            for(ServerInfo temp: secondaryMap){
                String host = temp.getHost();
                int port = temp.getPort();
                startHeartbeat(this.host, this.port, host, port);
            }

        }

        return ret;
    }

    /**
     * This method sends a RequestVoteRpc to all the other followers to request their votes. If the
     * count is equal to a majority, the process stops and a new leader is elected. Now you need
     * to send
     * @param sHost
     * @param sPort
     * @return
     */
    public boolean sendRequestVoteRpc(String sHost, int sPort){
        String url = "http://" + sHost + ":" + sPort + "/requestvote/";
        url = url.concat("term=" + term);
        int statusCode = 0;

        try {
            URL objUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) objUrl.openConnection();
            connection.setRequestMethod("GET");
            statusCode = connection.getResponseCode();
            System.out.println("status code when sending RequestVoteRpc to port " + sPort +
                    " = " + statusCode);
        }
        catch (ConnectException e){
            statusCode = 400;
        }
        catch (Exception e){ // in event of crash failure
            statusCode = 400;
        }

        return (statusCode == 200);
    }

    public void startHeartbeat(String primaryHost, int primaryPort, String secondaryHost, int secondaryPort){
        PrimaryFunctions primary = new PrimaryFunctions(secondaryMap, primaryHost, primaryPort);
        primary.sendHeartbeatsToSecondary(secondaryHost, secondaryPort);
    }

    /**
     * This class updates config file with new primary host/port
     * @param host
     * @param port
     * @throws IOException
     */
    public void changePrimaryConfig(String host, int port) throws IOException{
        Properties p = new Properties();
        p.load(new FileInputStream("config.properties"));
        p.put("leaderhost", host);
        p.put("leaderport", String.valueOf(port));
        p.store(new FileOutputStream("config.properties"), null);
    }

}
