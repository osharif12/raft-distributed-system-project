package Raft;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LogEntryList {
    private ArrayList<LogEntry> logEntryList;
    private ReentrantReadWriteLock lock;

    public LogEntryList(){
        logEntryList = new ArrayList<>();
        lock = new ReentrantReadWriteLock();
    }

    public LogEntryList(JSONArray jsonArray){
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject obj = (JSONObject) jsonArray.get(i);
            System.out.println("json object added to list = " + obj.toString());

            int term = (int)Long.parseLong(obj.get("term").toString());
            int index = (int)Long.parseLong(obj.get("index").toString());
            JSONObject temp = (JSONObject) obj.get("index");
            LogEntry entry = new LogEntry(term, index, temp);

            logEntryList.add(entry);
        }
    }

    /**
     * This method adds a log entry object to the threadsafe arraylist.
     * @param entry
     */
    public void addToList(LogEntry entry){
        lock.writeLock().lock();
        logEntryList.add(entry);
        lock.writeLock().unlock();
    }

    /**
     * Returns an arraylist of log entry objects
     * @return
     */
    public ArrayList<LogEntry> getList(){
        ArrayList<LogEntry> ret = new ArrayList<>();

        lock.readLock().lock();
        for(LogEntry entry: logEntryList){
            ret.add(entry);
        }
        lock.readLock().unlock();

        return ret;
    }

    /**
     * This function returns the term number of the last log entry in the LogEntryList
     * @return
     */
    public int getLastTerm(){
        LogEntry entry = null;

        lock.readLock().lock();
        int size = logEntryList.size();
        entry = logEntryList.get(size - 1);
        lock.readLock().unlock();

        return entry.getTerm();
    }

    /**
     * Method returns the last index of the last log entry in the list of log entries
     * @return
     */
    public int getLastIndex(){
        LogEntry entry = null;

        lock.readLock().lock();
        int size = logEntryList.size();
        entry = logEntryList.get(size - 1);
        lock.readLock().unlock();

        return entry.getIndex();
    }

}
