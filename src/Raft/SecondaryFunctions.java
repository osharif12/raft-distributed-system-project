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

public class SecondaryFunctions {
    private String host;
    private int port;
    private ArrayList<ServerInfo> secondaryMap;

    public SecondaryFunctions(String host1, int port1, ArrayList<ServerInfo> sMap){
        host = host1;
        port = port1;
        secondaryMap = sMap;
    }

    public void checkSecondary(PropertiesLoader properties, String isLeader) throws IOException {
        if(isLeader.equals("false")){ // is secondary event service, register w/ primary and get data
            // send a message to primary with port and ip of new secondary to start data transfer
            //log.debug("Secondary event service starting up at port " + secPort);
            System.out.println("Starting up secondary with port " + port);
            int leaderPort = Integer.valueOf(properties.getLeaderPort());
            String leaderHost = properties.getLeaderHost();

            // register secondary server with primary server and get primary information
            boolean register = registerSecondary(leaderHost, leaderPort, host, port, secondaryMap);

            System.out.println("List of all secondaries below(in secFunctions): ");
            for(ServerInfo temp: secondaryMap){
                System.out.println("Secondary with port = " + temp.getPort());
            }

            // register secondary with all other secondary nodes
            for(ServerInfo s: secondaryMap){
                String host1 = s.getHost();
                int port1 = s.getPort();
                boolean sendSecondaries = sendNewSecondary(host1, port1, host, port);
            }

            // while loop continuously sends heartbeat messages
            //Thread checkAlive = new Thread(new CheckAlive(primaryHost, primaryPort));
            //checkAlive.start();
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

}
