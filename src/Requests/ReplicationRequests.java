package Requests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import Raft.ServerInfo;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ReplicationRequests {

    public static void main(String[] args) throws Exception{
        boolean loop = true, createJson = true;
        ArrayList<ServerInfo> servers = getServerList();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Follow instructions below.");

        while(loop){
            System.out.println("1 to send a json object for storage");
            System.out.println("Enter exit to quit program");

            String input = scanner.next().trim();

            if(input.equals("1")){
                createJson = true;
                JSONObject object = new JSONObject();
                int iCount = 1, sCount = 1;
                System.out.println("1 to add an integer value");
                System.out.println("2 to add a string value");
                System.out.println("3 when you want to finish building json and send it");

                while(createJson){
                    System.out.print("Input: ");
                    String input2 = scanner.next().trim();

                    if(input2.equals("1")){
                        System.out.print("Integer input: ");
                        String input3 = scanner.next();
                        int num = Integer.valueOf(input3);
                        String name = "int" + iCount;
                        object.put(name, num);
                        iCount++;
                    }
                    else if(input2.equals("2")){
                        System.out.print("String input: ");
                        String input3 = scanner.next();
                        String name = "string" + sCount;
                        object.put(name, input3);
                        sCount++;
                    }
                    else if(input2.equals("3")){
                        createJson = false;

                        JSONObject finalObj = new JSONObject();
                        finalObj.put("lastTerm", "1");
                        finalObj.put("lastIndex", "1");
                        finalObj.put("data", object);

                        JSONObject leaderJson = requestLeaderInfo(servers);

                        // arguments include json you want to send and json with leader info
                        boolean send = sendAppendEntryRpc(finalObj, leaderJson);
                        System.out.println("sending json object, send status = " + send);
                    }
                    else{
                        System.out.println("Invalid input, please try again");
                    }
                } // end of create json while loop
            }
            else if(input.equals("exit")){
                loop = false;
            }
            else{
                System.out.println("Invalid input, please try again");
            }

        } // end of

    }

    public static ArrayList<ServerInfo> getServerList(){
        ArrayList<ServerInfo> ret = new ArrayList<>();
        String host = "localhost"; // run tunneling for each

        //int port1 = 4450;
        int port1 = 8000;
        ServerInfo temp1 = new ServerInfo(port1, host);
        ret.add(temp1);

        int port2 = 8001;
        ServerInfo temp2 = new ServerInfo(port2, host);
        ret.add(temp2);

        int port3 = 8002;
        ServerInfo temp3 = new ServerInfo(port3, host);
        ret.add(temp3);

        int port4 = 8003;
        ServerInfo temp4 = new ServerInfo(port4, host);
        ret.add(temp4);

        int port5 = 8004;
        ServerInfo temp5 = new ServerInfo(port5, host);
        ret.add(temp5);

        return ret;
    }

    public static boolean sendAppendEntryRpc(JSONObject object, JSONObject leaderInfo) throws IOException {
        String host = leaderInfo.get("leaderhost").toString();
        int port = Integer.valueOf(leaderInfo.get("leaderport").toString());
        String url = "http://" + host + ":" + port + "/appendentry";
        int statusCode = 0;

        try{
            URL objUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) objUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-type", "application/json");

            // sending json object in post request
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(object.toString());
            writer.flush();

            statusCode = connection.getResponseCode();
            if(statusCode == 200){
                System.out.println("Status code when sending json object to leader with port = " + statusCode);
                System.out.println("Successfully committed to leader and majority of followers");
            }
            else{
                System.out.println("Could not successfully commit the json object to data store");
            }
        }
        catch(Exception e){
            System.out.println("Service with port " + port + " is offline");
            statusCode = 400;
        }

        return (statusCode == 200);
    }


    /**
     * In this method you will contact random servers in the list to find the leader info
     * @param serversList
     * @return
     * @throws Exception
     */
    public static JSONObject requestLeaderInfo(ArrayList<ServerInfo> serversList) {
        boolean loop = true;
        JSONObject ret = null;
        int i = 0;
        while(loop){
            // if jsonObject you get back is not empty, then break out of loop and return json object
            ServerInfo temp = serversList.get(i);
            String host = temp.getHost();
            int port = temp.getPort();

            try {
                String url = "http://" + host + ":" + port + "/leaderinfo";
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setDoOutput(true);
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");

                JSONObject object = new JSONObject();
                object.put("loginfo", 1);

                OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
                writer.write(object.toString());
                writer.flush();

                int statusCode = con.getResponseCode();
                System.out.println("Status code for getting leader info = " + statusCode);

                if (statusCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String line;
                    StringBuilder builder = new StringBuilder();

                    while ((line = in.readLine()) != null) {
                        builder.append(line);
                    }

                    String jsonString = builder.toString();
                    System.out.println("Leader json info = " + jsonString);
                    JSONParser parser = new JSONParser();
                    JSONObject temp1 = (JSONObject) parser.parse(jsonString);

                    String leaderport = "", leaderhost = "";
                    leaderhost = temp1.get("leaderhost").toString();
                    leaderport = temp1.get("leaderport").toString();

                    // This function changes the leader port to the port that corresponds to it in the tunneling command
                    leaderport = changeLeaderPort(leaderport);

                    leaderhost = "localhost";
                    JSONObject temp2 = new JSONObject();
                    temp2.put("leaderhost", leaderhost);
                    temp2.put("leaderport", leaderport);
                    ret = temp2;

                    loop = false;
                }
            }
            catch(Exception e){
                System.out.println("Server is down, contacting another");
            }
            finally {
                i++;
            }
        } // end of while loop

        return ret;
    }

    public static String changeLeaderPort(String leaderport){
        if(leaderport.equals("4450")){
            leaderport = "8000";
        }
        else if(leaderport.equals("4451")){
            leaderport = "8001";
        }
        else if(leaderport.equals("4452")){
            leaderport = "8002";
        }
        else if(leaderport.equals("4453")){
            leaderport = "8003";
        }
        else if(leaderport.equals("4454")){
            leaderport = "8004";
        }

        return leaderport;
    }

}
