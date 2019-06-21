package server.chat;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * receives messages from client
 */
class ConnectionListener implements Runnable{
    /**
     * thread
     */
    private Thread t;
    /**
     * chat's listener socket
     */
    private Socket socket;
    /**
     * used in main thread loop
     */
    private boolean loop = true;
    /**
     * reference to ChatServer
     * used to send messages to server
     */
    private ChatServer cm;

    /**
     *
     * @param socket listener socket
     * @param cm reference to ChatServer thread
     */
    ConnectionListener(Socket socket, ChatServer cm){
        this.socket = socket;
        this.cm = cm;
        if (t == null) {
            t = new Thread(this, "ConnectionListener_"+socket.getPort());
        }

    }
    @Override
    public void run() {
        InputStream input = null;
        Scanner in = null;
        try{
            input = socket.getInputStream();
            in = new Scanner(input);
        }catch(IOException e){
            System.out.println("couldnt socket.getInputStream");
            loop = false;
        }
        while(loop){
            if(socket.isClosed()){
                break;
            }
            if(in.hasNextLine()){
                String msg = in.nextLine();
                if(msg.equals("!exit")){
                    this.stop();
                }else {
                    if(msg.length() <= 50) {
                        System.out.println(msg);
                        cm.receive(msg);
                    }
                }
            }
        }
    }
    public void stop(){
        loop = false;
        try {
            socket.close();
        }catch(Exception e){}
        //t.interrupt();
    }
}
