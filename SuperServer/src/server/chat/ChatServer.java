package server.chat;

import server.Controller;

import java.util.ArrayList;


/**
 * main chat server thread
 */
public class ChatServer implements Runnable{
    private static int listenerPort;
    private static int speakerPort;
    private Thread t;
    private ThreadPoolListener listenerThread;
    private ThreadPoolSpeaker speakerThread;
    private Controller c;
    private ArrayList<String> msgs;

    boolean isRunning;

    /**
     *
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
