import java.util.*;

public class LogEntry {
    private int term;
    private int index;
    private int data;

    public LogEntry(int term1, int index1, int data1){
        term = term1;
        index = index1;
        data = data1;
    }

    public int getTerm(){
        return term;
    }

    public int getIndex(){
        return index;
    }

    public int getData(){
        return data;
    }

}
