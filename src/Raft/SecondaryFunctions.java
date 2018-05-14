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

/**
 * This class is created for all services, it has a number of functions. For all services it will
 * keep track of the host, port, current term, secondaryMap, and current index of all services.
 * For the followers it will register itself with the leader, get a list of all other followers
 * from the leader, and register itself with all other followers. Lastly once this follower receives
 * its first heartbeat message from the leader, it will fire off a thread in the startTimer()
 * method to generate a random timeout and keep listening for the heartbeat messages from the
 * leader.
 */
public class SecondaryFunctions{
    private String host;
    private int port;
    private ArrayList<ServerInfo> secondaryMap;
    private boolean resetTimer;
    private boolean timerSet;
    private volatile int term;
    private boolean isLeader;
    private int index;

    public SecondaryFunctions(String host1, int port1, ArrayList<ServerInfo> sMap){
        host = host1;
        port = port1;
        secondaryMap = sMap;
        //timerSet = timer;
        timerSet = false;
        resetTimer = true;
        term = 0;
        index = 0;
        isLeader = false;
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

    public synchronized int getIndex(){
        return index;
    }

    public synchronized void incrementIndex(){
        index++;
    }

    public boolean getLeader(){
        return isLeader;
    }

    public void setLeader(boolean leader){
        isLeader = leader;
    }

    public synchronized boolean getTimerSet(){
        return timerSet;
    }

    public synchronized void setTimerSet(boolean t){
        timerSet = t;
    }

    /**
     * This method fires off a thread that starts a timer with a randomized election timeout
     * between 150-300 ms. As long as the leader sends appendRPC messages every 150 ms then the
     * follower will accept the leader exists. If election timeout occurs, follower will start
     * election.
     */
    public void startTimer() throws InterruptedException{
        System.out.println("Secondary starting timer");

        Thread timer = new Thread(new ReceiveHeartbeat());
        timer.start();
    }

    /**
     * This class will reset the timer(while loop) by setting the resetTimer flag back to true
     */
    public void resetTimer(){
        resetTimer = true;
    }

    public void startElection(){
        System.out.println("Primary server crashed, starting election, term = " + term);
        // set timerSet to false so secondary can create new timer with new primary
        setTimerSet(false);

        incrementTerm();
        Election election = new Election(host, port, secondaryMap, term, this);
        boolean elect = election.electLeader();

        if(elect){
            //System.out.println("This node was elected leader");
            // set this secondary to leader
            setLeader(true);
        }
        else{
            System.out.println("This node was not elected leader");
        }
    }

    private class ReceiveHeartbeat implements Runnable{
        private int randomNumber;

        public ReceiveHeartbeat(){
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
                        this.wait(randomNumber + 20);
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
