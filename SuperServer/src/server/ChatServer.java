package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    public static void main(String[] args){
        ChatManager cm = new ChatManager();
        cm.start();

    }
}

class ChatManager implements Runnable{
    private static int listenerPort = 55556;
    private static int speakerPort = 55557;
    private Thread t;
    private boolean loop = true;
    private ThreadPoolListener listenerThread;
    private ThreadPoolSpeaker speakerThread;

    private ArrayList<String> msgs;

    ChatManager() {
        //this.start();
        msgs = new ArrayList<>();
        System.out.println("ChatManager started");
    }

    @Override
    public void run() {
        //create ThreadPoolListener
        listenerThread = new ThreadPoolListener(listenerPort, 100, this);
        listenerThread.start();
        //create ThreadPooSpeaker
        speakerThread = new ThreadPoolSpeaker(speakerPort, 100, this);
        speakerThread.start();

        while(loop){

        }

        System.out.println("ChatManager ended");
    }
    public synchronized void receive(String msg){
        msgs.add(msg);
    }
    public ArrayList<String> send(){
        return msgs;
    }
    public void start(){
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }
    public void stop(){
        loop = false;
        listenerThread.stop();
        speakerThread.stop();
        t.interrupt();
    }
}

//thread with pool listeners
class ThreadPoolListener implements Runnable{
    private Thread t;
    private boolean flag = true;
    private int port, nThreads;
    private ServerSocket listener;
    private ExecutorService pool;
    private ChatManager cm;

    ThreadPoolListener(int ports, int nThreads, ChatManager cm) {
        this.port = ports;
        this.nThreads = nThreads;
        this.cm = cm;
        System.out.println("ThreadPoolListener started.");
    }

    @Override
    public void run() {
        pool = null;
        listener = null;
        int maxTries = 10;
        while (maxTries > 0) {
            maxTries -= 1;
            try {
                listener = new ServerSocket(port);
                pool = Executors.newFixedThreadPool(nThreads);
                break;
            } catch (Exception e) {
                System.out.println("Failed to create server socket " + e.getMessage() + "\nTrying again in 10s ...\n");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ee) {

                }
            }
        }
        if (maxTries <= 0) return;
        System.out.println("ThreadPoolListener is running.");
        while (flag) {
            try {
                pool.execute(new ConnectionListener(listener.accept(), cm));
            } catch (Exception e) {
                if (!flag) break;
                System.out.println("Failed to add new ConnectionListener");
            }
        }
        System.out.println("ThreadPoolListener end");

    }
    public void start(){
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }
    public void stop() {
        flag = false;
        try{
            listener.close();
        }catch(Exception e){};
        pool.shutdown();
        t.interrupt();
    }
}

//thread with pool speakers
class ThreadPoolSpeaker implements Runnable{
    private Thread t;
    private boolean flag = true;
    private int port, nThreads;
    private ServerSocket listener;
    private ExecutorService pool;
    private ChatManager cm;

    ThreadPoolSpeaker(int ports, int nThreads, ChatManager cm) {
        this.port = ports;
        this.nThreads = nThreads;
        this.cm = cm;
        System.out.println("ThreadPoolSpeaker started.");
    }

    @Override
    public void run() {
        pool = null;
        listener = null;
        int maxTries = 10;
        while (maxTries > 0) {
            maxTries -= 1;
            try {
                listener = new ServerSocket(port);
                pool = Executors.newFixedThreadPool(nThreads);
                break;
            } catch (Exception e) {
                System.out.println("Failed to create server socket " + e.getMessage() + "\nTrying again in 10s ...\n");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ee) {

                }
            }
        }
        if (maxTries <= 0) return;
        System.out.println("ThreadPoolSpeaker is running.");
        while (flag) {
            try {
                pool.execute(new ConnectionSpeaker(listener.accept(), cm));
            } catch (Exception e) {
                if (!flag) break;
                System.out.println("Failed to add new ConnectionSpeaker");
            }
        }
        System.out.println("ThreadPoolSpeaker end");

    }
    public void start(){
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }
    public void stop() {
        flag = false;
        try{
            listener.close();
        }catch(Exception e){};
        pool.shutdown();
        t.interrupt();
    }
}

//ConnectionListener
//receives msg from client
class ConnectionListener implements Runnable{
    private Thread t;
    private Socket socket;
    private boolean loop = true;
    private ChatManager cm;

    ConnectionListener(Socket socket, ChatManager cm){
        this.socket = socket;
        this.cm = cm;
        //this.start();
        if (t == null) {
            t = new Thread(this);
            //t.start();
        }
        System.out.println("ConnectionSpeaker thread started");

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
                System.out.println(msg);
                cm.receive(msg);
            }

        }
    }
    public void stop(){
        loop = false;
        t.interrupt();
    }
}
//ConnectionSpeaker
//sends msg to client
class ConnectionSpeaker implements Runnable{
    private Thread t;
    private Socket socket;
    private boolean loop = true;
    private PrintWriter out;
    private ChatManager cm;

    ConnectionSpeaker(Socket socket, ChatManager cm){
        this.socket = socket;
        this.cm = cm;
        //this.start();
        if (t == null) {
            t = new Thread(this);
            //t.start();
        }
        System.out.println("ConnectionSpeaker thread started");
    }

    @Override
    public void run() {
        out = null;
        try{
            out = new PrintWriter(socket.getOutputStream(), true);
        }catch (IOException e){
            System.out.println("ConnectionSpeaker : IOException: failed to get output stream from client");
        }
        int n = 0;
        while(loop){
            //if stream has next line
            if(socket.isClosed()){
                break;
            }
            ArrayList<String> msgs = cm.send();
            if(n != msgs.size()){
                //is new message
                for(int i = n; i < msgs.size(); i++){
                    speak(msgs.get(i));
                }
                n = msgs.size();
            }
            try{
                Thread.sleep(1000);
            }catch(Exception e){}
        }

    }
    public void speak(String msg){
        try{
            out.println(msg);
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }
    public void stop(){
        loop = false;
        t.interrupt();
    }


}