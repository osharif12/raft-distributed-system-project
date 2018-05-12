package Raft;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class PrimaryFunctions {
    private ArrayList<ServerInfo> secondaryMap;
    private String primaryHost;
    private int primaryPort;

    public PrimaryFunctions(ArrayList<ServerInfo> sMap, String pHost, int pPort){
        secondaryMap = sMap;
        primaryHost = pHost;
        primaryPort = pPort;
    }

    public void sendHeartbeatsToSecondary(String secondaryHost, int secondaryPort){
        Thread checkAlive = new Thread(new CheckAlive(secondaryHost, secondaryPort));
        checkAlive.start();
    }

    public boolean sendHeartbeat(String secondaryHost, int secondaryPort) throws IOException{
        String url = "http://" + secondaryHost + ":" + secondaryPort + "/appendentry";
        int statusCode = 0;

        try{
            URL objUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) objUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-type", "application/json");
            statusCode = connection.getResponseCode();
            System.out.println("sent heartbeat from primary to secondary with port " + secondaryPort);
        }
        catch(Exception e){
            System.out.println("secondary with port " + secondaryPort + " is offline");
            statusCode = 400;
        }

        return (statusCode == 200);
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
