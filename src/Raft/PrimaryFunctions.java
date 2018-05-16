package Raft;

import org.json.simple.JSONObject;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * This class has a set of functions that only the leader in Raft will implement. These functions
 * include sending empty AppendEntryRpc's to all followers every 150 ms.
 */
public class PrimaryFunctions {
    private SecondaryFunctions secondary;
    private ArrayList<ServerInfo> secondaryMap;
    private String primaryHost;
    private int primaryPort;

    public PrimaryFunctions(SecondaryFunctions sec, ArrayList<ServerInfo> sMap, String pHost, int pPort){
        secondary = sec;
        secondaryMap = sMap;
        primaryHost = pHost;
        primaryPort = pPort;
    }

    /**
     * This method fires off a new thread whose job it is to send a heartbeat message/Appendentryrpc
     * with no data to a follower.
     * @param secondaryHost
     * @param secondaryPort
     */
    public void sendHeartbeatsToSecondary(String secondaryHost, int secondaryPort){
        Thread checkAlive = new Thread(new CheckAlive(secondaryHost, secondaryPort));
        checkAlive.start();
    }

    /**
     * This class is called by the private inner class to send an AppendEntryRPC without data
     * (heartbeat) to the specified follower.
     * @param secondaryHost
     * @param secondaryPort
     * @return
     * @throws IOException
     */
    public boolean sendHeartbeat(String secondaryHost, int secondaryPort) throws IOException{
        String url = "http://" + secondaryHost + ":" + secondaryPort + "/appendentry";
        int statusCode = 0;

        try{
            URL objUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) objUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-type", "application/json");

            JSONObject object = createJson();

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(object.toString());
            writer.flush();

            statusCode = connection.getResponseCode();
        }
        catch(Exception e){
            statusCode = 400;
        }

        return (statusCode == 200);
    }

    /**
     * This method creates a json by getting the term and index of the last entry in the log. The
     * data json will be empty signifying that it is a heartbeat appendEntryRpc
     * @return
     */
    public JSONObject createJson(){
        JSONObject object = new JSONObject();
        int lastTerm = secondary.getTerm();
        int lastIndex = secondary.getIndex(); // should be 0 if nothing is on log
        JSONObject data = new JSONObject();

        object.put("lastTerm", String.valueOf(lastTerm));
        object.put("lastIndex", String.valueOf(lastIndex));
        object.put("data", data); // this will be empty json

        return object;
    }

    private class CheckAlive implements Runnable{
        String secondaryHost;
        int secondaryPort;

        public CheckAlive(String host, int port){
            secondaryHost = host;
            secondaryPort = port;
        }

        @Override
        public void run(){
            // while loop continuously sends heartbeat messages to secondary, even if server is down
            boolean alive = true;

            while(alive){
                synchronized (this) {
                    try {
                        alive = sendHeartbeat(secondaryHost, secondaryPort);
                        // keep trying to send to follower even if follower is down
                        if(alive == false){
                            alive = true;
                        }

                        this.wait(150);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } // end of while
        }
    } // end of CheckAlive class

}
