package Raft;

import org.json.simple.JSONObject;
import java.util.*;

public class LogEntry {
    private int term;
    private int index;
    private JSONObject object;

    public LogEntry(int term1, int index1, JSONObject object1){
        term = term1;
        index = index1;
        object = object1;
    }

    public int getTerm(){
        return term;
    }

    public int getIndex(){
        return index;
    }

    public JSONObject getData(){
        return object;
    }

    public JSONObject returnJson(){
        JSONObject ret = new JSONObject();

        ret.put("term", term);
        ret.put("index", index);
        ret.put("data", object);

        return ret;
    }

}
