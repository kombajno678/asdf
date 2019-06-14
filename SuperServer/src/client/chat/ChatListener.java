package client.chat;
import client.Controller;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * Listens for any new chat messages from server and sends them to gui
 */
public class ChatListener implements Runnable{
    private Thread t;
    private boolean loop;
    private Socket socket;
    private String ip;
    private int port;
    private Scanner in;
    private Controller c;

    /**
     * @param ip server's ip
     * @param port server's chat speaker port
     * @param c reference to gui controller
     */
    public ChatListener(String ip, int port, Controller c) {
        this.ip = ip;
        this.port = port;
        this.c = c;
        loop = true;
    }
    @Override
    public void run() {
        System.out.println("ChatListener start");
        //try to connect
        //if not connected, try again in 1s
        //c.displayMsg("Listener> Connecting to chat server ...");
        while(socket == null && loop){
            createSocket();
            if(socket == null){
                try{
                    Thread.sleep(1000);
                }catch(Exception e){}
            }
        }
        //if(loop)c.displayMsg("Listener> Connected!");
        while(loop){
            try{
                if(in.hasNextLine()){
                    c.displayMsg(in.nextLine());
                }
            }catch(Exception e){
                System.out.println("Listener> "+e.getMessage());
                stop();
            }
        }
        System.out.println("ChatListener end");
    }

    /**
     * starts thread
     */
    public void start(){
        if (t == null) {
            t = new Thread (this, "ChatListener");
            t.start ();
        }
    }

    /**
     * tries to stop thread
     */
    public void stop(){
        try {
            //new PrintWriter(socket.getOutputStream()).println("!exit");
            socket.close();
        }catch(Exception e){}

        loop = false;
        t.interrupt();
    }

    /**
     * creates socket
     */
    private void createSocket(){
        try {
            socket = new Socket(ip, port);
        } catch (Exception e) {
            return;
        }
        in = null;
        try {
            in = new Scanner(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("Listener> Failed to get output stream from server");
        }

    }
}
