package server.chat;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * receives messages from client
 */
class ConnectionListener implements Runnable{
    private Thread t;
    private Socket socket;
    private boolean loop = true;
    private ChatServer cm;

    /**
     *
     * @param socket listener socket
     * @param cm reference to ChatServer thread
     */
    ConnectionListener(Socket socket, ChatServer cm){
        this.socket = socket;
        this.cm = cm;
        //this.start();
        if (t == null) {
            t = new Thread(this, "ConnectionListener_"+socket.getPort());
            //t.start();
        }
        //System.out.println("ConnectionSpeaker thread started");

    }
    @Override
    public void run() {
        //System.out.println("ConnectionListener Running "+t.getId());
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
                    //System.out.println("===========================================");
                    this.stop();
                }else {
                    if(msg.length() <= 50) {
                        System.out.println(msg);
                        cm.receive(msg);
                    }
                }
            }

        }
        //System.out.println("ConnectionListener Stopped "+t.getId());
    }
    public void stop(){
        loop = false;
        try {
            socket.close();
        }catch(Exception e){}
        //t.interrupt();
    }
}
