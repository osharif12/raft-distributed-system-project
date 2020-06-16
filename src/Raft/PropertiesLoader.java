package Raft;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {
    private String leaderhost;
    private String leaderport;


    public PropertiesLoader() {
        loadProperties();
    }

    private void loadProperties() {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream("config.properties");

            // load a properties file
            prop.load(input);
            this.leaderhost = prop.getProperty("leaderhost");
            this.leaderport = prop.getProperty("leaderport");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLeaderHost() {
        return leaderhost;
    }

    public String getLeaderPort() {
        return leaderport;
    }
}
