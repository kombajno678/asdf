package server.chat;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 * sends messages to client
 */
class ConnectionSpeaker implements Runnable{
    /**
     * thread
     */
    private Thread t;
    /**
     * chat's speaker socket
     */
    private Socket socket;
    /**
     * user in main thread loop
     */
    private boolean loop = true;
    /**
     * socket's output stream
     */
    private PrintWriter out;
    /**
     * reference to ChatServer
     * used to check if server is running
     */
    private ChatServer cm;
    /**
     *
     * @param socket listener socket
     * @param cm reference to ChatServer thread
     */
    ConnectionSpeaker(Socket socket, ChatServer cm){
        this.socket = socket;
        this.cm = cm;
        if (t == null) {
            t = new Thread(this, "ConnectionSpeaker_"+socket.getPort());
        }
    }

    @Override
    public void run() {
        out = null;
        try{
            out = new PrintWriter(socket.getOutputStream(), true);
        }catch (IOException e){
            System.out.println("ConnectionSpeaker : IOException: failed to get stream from client");
        }
        int n = 0;
        while(loop & cm.isRunning){
            //if stream has next line
            ArrayList<String> msgs = cm.send();
            if(n != msgs.size()){
                //is new message
                if(n > msgs.size()){
                    //list of msgs has been cleared
                    n = 0;
                }
                for(int i = n; i < msgs.size(); i++){
                    speak(msgs.get(i));
                }
                n = msgs.size();
            }
            try{
                Thread.sleep(1000);
            }catch(Exception e){
                break;
            }
        }
    }

    /**
     * sends msg to client
     * @param msg message to send
     */
    public void speak(String msg){
        try{
            out.println(msg);
        }catch(Exception e){
            System.out.println(e.getMessage());
            this.stop();
        }
    }
    public void stop(){
        loop = false;
        try{
            socket.close();
        }catch(Exception e){
            System.out.println("ConnectionSpeaker Exception "+e.getMessage());
        }
        t.interrupt();
    }
}
