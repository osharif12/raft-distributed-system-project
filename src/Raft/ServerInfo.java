package Raft;

public class ServerInfo {
    private int port;
    private String host;

    public ServerInfo(int port, String host){
        this.port = port;
        this.host = host;
    }

    public int getPort(){
        return port;
    }

    public String getHost(){
        return host;
    }
}
