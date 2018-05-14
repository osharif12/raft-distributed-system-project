package Requests;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import Raft.PropertiesLoader;
import org.json.simple.JSONObject;

public class ReplicationRequests {

    public static void main(String[] args) throws IOException{
        boolean loop = true, createJson = true;
        int MAX_VALUE = 2147483647;
        int MIN_VALUE = -2147483648;
        PropertiesLoader propertiesLoader = new PropertiesLoader();
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

                        boolean send = sendAppendEntryRpc(propertiesLoader, object);
                        System.out.println("sending json object, response code = " + send);
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

    public static boolean sendAppendEntryRpc(PropertiesLoader propertiesLoader, JSONObject object) throws IOException {
        String host = propertiesLoader.getLeaderHost();
        int port = Integer.valueOf(propertiesLoader.getLeaderPort());
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



}
