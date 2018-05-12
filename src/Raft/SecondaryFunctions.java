package Raft;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class SecondaryFunctions{
    private String host;
    private int port;
    private ArrayList<ServerInfo> secondaryMap;
    private boolean resetTimer = true;
    private boolean timerSet;
    private volatile int term;

    public SecondaryFunctions(String host1, int port1, ArrayList<ServerInfo> sMap, boolean timer){
        host = host1;
        port = port1;
        secondaryMap = sMap;
        timerSet = timer;
        term = 0;
    }

    public void checkSecondary(PropertiesLoader properties, String isLeader) throws IOException {
        if(isLeader.equals("false")){ // is secondary event service, register w/ primary and get data
            System.out.println("Starting up secondary with port " + port);
            int leaderPort = Integer.valueOf(properties.getLeaderPort());
            String leaderHost = properties.getLeaderHost();

            // register secondary server with primary server and get primary information
            boolean register = registerSecondary(leaderHost, leaderPort, host, port, secondaryMap);

            System.out.println("List of all secondaries below(in secFunctions): ");
            for(ServerInfo temp: secondaryMap){
                System.out.println("Secondary with port = " + temp.getPort());
            }

            // register follower with all other follower nodes
            for(ServerInfo s: secondaryMap){
                String host1 = s.getHost();
                int port1 = s.getPort();
                boolean sendSecondaries = sendNewSecondary(host1, port1, host, port);
            }
        }

    }

    public boolean registerSecondary(String primHost, int primPort, String host, int port,
                                     ArrayList<ServerInfo> secondaryMap) throws IOException{
        String url = "http://" + primHost + ":" + primPort + "/register/";
        url = url.concat("host=" + host + "port=" + port);
        int statusCode = 0;

        try{
            URL objUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) objUrl.openConnection();
            connection.setRequestMethod("GET");
            statusCode = connection.getResponseCode();
            String jsonString = getResponse(connection);
            System.out.println("jsonString received from primary = " + jsonString);

            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
            JSONArray secondaryArray = (JSONArray) jsonObject.get("secondaries");

            // Iterate through the JsonArray and add all the secondary servers to map
            Iterator<JSONObject> iterator = secondaryArray.iterator();
            while (iterator.hasNext()) {
                JSONObject object = iterator.next();

                int port1 = (int)Long.parseLong(object.get("port").toString());
                String host1 = object.get("host").toString();
                if(this.port != port1){
                    ServerInfo serverInfo = new ServerInfo(port1, host1);
                    secondaryMap.add(serverInfo);
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return (statusCode == 200);
    }

    public String getResponse(HttpURLConnection connection) throws IOException{
        // get the json array response from GET request that was sent
        String inputLine;
        StringBuffer response = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        return response.toString();
    }

    public boolean sendNewSecondary(String secondaryHost, int secondaryPort, String host, int port){
        String url = "http://" + secondaryHost + ":" + secondaryPort + "/newsecondary/";
        url = url.concat("host=" + host + "port=" + port);
        int statusCode = 0;

        try {
            URL objUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) objUrl.openConnection();
            connection.setRequestMethod("GET");
            statusCode = connection.getResponseCode();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return (statusCode == 200);
    }

    public synchronized int getTerm(){
        return term;
    }

    public synchronized void incrementTerm(){
        term++;
    }

    /**
     * This method fires off a thread that starts a timer with a randomized election timeout
     * between 150-300 ms. As long as the leader sends appendRPC messages every 150 ms then the
     * follower will accept the leader exists. If election timeout occurs, follower will start
     * election.
     */
    public void startTimer() throws InterruptedException{
        //term = term1;
        System.out.println("Secondary starting timer");

        /*
        Random random = new Random();
        int randomNumber = random.nextInt(151) + 150;
        System.out.println("random wait number generated for port " + port + " = " + randomNumber + " ms");

        while(resetTimer){
            resetTimer = false;
            Thread.sleep(randomNumber);
            //this.wait(randomNumber);

            if(!resetTimer){
                startElection();
            }
        }
    */
        Thread timer = new Thread(new ReceiveHeartbeat());
        timer.start();
        //timer.join();
    }

    /**
     * This class will reset the timer(while loop) by setting the resetTimer flag back to true
     */
    public void resetTimer(){
        //term = term1;
        resetTimer = true;
    }

    public void startElection(){
        System.out.println("Primary server crashed, starting election, term = " + term);
        // set timerSet to false so secondary can create new timer with new primary
        timerSet = false;

        incrementTerm();
        Election election = new Election(host, port, secondaryMap, term);
        boolean elect = election.electLeader();

        if(elect){
            //System.out.println("This node was elected leader");
        }
        else{
            System.out.println("This node was not elected leader");
        }
    }

    private class ReceiveHeartbeat implements Runnable{
        private int randomNumber;
        //private volatile int term;

        public ReceiveHeartbeat(){
            //term = term1;
            Random random = new Random();
            randomNumber = random.nextInt(151) + 200;
            System.out.println("random wait number generated for port " + port + " = " + randomNumber + " ms");
        }

        @Override
        public void run(){
            while(resetTimer){
                synchronized (this) {
                    resetTimer = false;
                    try{
                        this.wait(randomNumber);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                    if(!resetTimer){ // start election if primary crashed
                        startElection();
                        /*
                        System.out.println("Primary server crashed, starting election, term = " + term);
                        // set timerSet to false so secondary can create new timer with new primary
                        timerSet = false;

                        Election election = new Election(host, port, secondaryMap, term);
                        boolean elect = election.electLeader();

                        if(elect){
                            //System.out.println("This node was elected leader");
                        }
                        else{
                            System.out.println("This node was not elected leader");
                        }
                        */
                    }
                }
            } // end of while
            System.out.println("Thread is dead");
        }
    } // end of ReceiveHeartbeat class

}
