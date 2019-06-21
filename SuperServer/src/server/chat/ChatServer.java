package server.chat;

import server.Controller;

import java.util.ArrayList;


/**
 * main chat server thread
 */
public class ChatServer implements Runnable{
    /**
     * chat's listener port
     */
    private static int listenerPort;
    /**
     * chat's speaker port
     */
    private static int speakerPort;
    /**
     * thread
     */
    private Thread t;
    /**
     * listeners thread pool
     */
    private ThreadPoolListener listenerThread;
    /**
     * speakers thread pool
     */
    private ThreadPoolSpeaker speakerThread;
    /**
     * reference to gui controller
     */
    private Controller c;
    /**
     * list of all chat messages
     */
    private ArrayList<String> msgs;
    /**
     * tells if chat server is running
     */
    boolean isRunning;

    /**
     * public constructor
     * @param port main server's port, user to create chat's speaker and listener port
     * @param c reference to gui controller
     */
    public ChatServer(int port, Controller c) {
        this.c = c;
        listenerPort = port + 1;
        speakerPort = port + 2;
        msgs = new ArrayList<>();
    }

    @Override
    public void run() {
        System.out.println("ChatServer started");
        //create ThreadPoolListener
        listenerThread = new ThreadPoolListener(listenerPort, 100, this);
        listenerThread.start();
        //create ThreadPooSpeaker
        speakerThread = new ThreadPoolSpeaker(speakerPort, 100, this);
        speakerThread.start();
        try{
            listenerThread.t.join();
            speakerThread.t.join();
        }catch(Exception e){}
        System.out.println("ChatServer stopped");
    }

    /**
     * adds message to list of messages
     * displays message in gui
     * @param msg received message
     */
    public synchronized void receive(String msg){
        msgs.add(msg);
        c.displayMsg(msg);
    }

    /**
     * basically getter of msgs
     * @return list of messages
     */
    public ArrayList<String> send(){
        return msgs;
    }
    public void start(){
        isRunning = true;
        if (t == null) {
            t = new Thread (this, "ChatServer_Thread");
            t.start ();
        }
    }
    public void stop(){
        isRunning = false;
        listenerThread.stop();
        speakerThread.stop();
        t.interrupt();
    }

    /**
     * clears list of messages
     */
    public void clear(){
        msgs.removeAll(msgs);
    }
}
