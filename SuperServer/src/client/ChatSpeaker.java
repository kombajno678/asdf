package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatSpeaker implements Runnable {
    private Thread t;
    private boolean loop;
    private Socket socket;
    private String ip;
    private int port;
    private Controller c;
    private PrintWriter out;

    public ChatSpeaker(String ip, int port, Controller c) {
        this.ip = ip;
        this.port = port;
        this.c = c;
        loop = true;
    }
    @Override
    public void run() {
        System.out.println("ChatSpeaker start");
        //try to connect
        //if not connected, try again in 1s
        c.displayMsg("Connecting to chat server ...");
        while(socket == null && loop){
            createSocket();
            if(socket == null){
                try{
                    Thread.sleep(1000);
                }catch(Exception e){}
            }
        }
        if(loop)c.displayMsg("Connected!");
        //go sleep
        while(loop){
            try{
                Thread.sleep(Long.MAX_VALUE);
            }catch(Exception e){}
        }
        System.out.println("ChatSpeaker end");
    }
    public void start(){
        if (t == null) {
            t = new Thread (this, "ChatSpeaker");
            t.start ();
        }
    }
    public void stop(){
        try{
            out.println("!exit");
            socket.close();
        }catch(Exception e){}
        loop = false;
        t.interrupt();
    }
    private void createSocket(){
        try {
            socket = new Socket(ip, port);
        } catch (Exception e) {
            return;
        }
        out = null;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Speaker> Failed to get output stream from server");
        }

    }

    public void sendMsg(String msg){
        try{
            out.println(msg);
        }catch(Exception e){}
    }
}
