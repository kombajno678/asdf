package server.chat;


//ConnectionSpeaker

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 * sends messages to client
 */
class ConnectionSpeaker implements Runnable{
    private Thread t;
    private Socket socket;
    private boolean loop = true;
    private PrintWriter out;
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
        //System.out.println("ConnectionSpeaker Starting "+t.getId());
        out = null;
        try{
            out = new PrintWriter(socket.getOutputStream(), true);
        }catch (IOException e){
            System.out.println("ConnectionSpeaker : IOException: failed to get stream from client");
        }
        int n = 0;
        //System.out.println("ConnectionSpeaker Running "+t.getId());
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
        //System.out.println("ConnectionSpeaker stopped "+t.getId());
    }

    /**
     * sends msg to client
     * @param msg message to send
     */
    public void speak(String msg){
        //System.out.println("ConnectionSpeaker SPEAKING "+t.getId());
        try{
            out.println(msg);
        }catch(Exception e){
            System.out.println(e.getMessage());
            this.stop();
        }
    }
    public void stop(){
        //System.out.println("ConnectionSpeaker stopping "+t.getId());

        loop = false;
        try{
            socket.close();
        }catch(Exception e){
            System.out.println("ConnectionSpeaker Exception "+e.getMessage());
        }
        t.interrupt();
    }


}
