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

class ChatServer implements Runnable{
    private static int listenerPort;
    private static int speakerPort;
    private Thread t;
    private ThreadPoolListener listenerThread;
    private ThreadPoolSpeaker speakerThread;
    private Controller c;
    private ArrayList<String> msgs;

    public boolean isRunning;

    ChatServer(int port, Controller c) {
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
    public synchronized void receive(String msg){
        msgs.add(msg);
        c.displayMsg(msg);
    }
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
    public void clear(){
        msgs.removeAll(msgs);
    }
}

//thread with pool listeners
class ThreadPoolListener implements Runnable{
    protected Thread t;
    private boolean flag = true;
    private int port, nThreads;
    private ServerSocket socket;
    private ExecutorService pool;
    private ChatServer cm;

    ThreadPoolListener(int ports, int nThreads, ChatServer cm) {
        this.port = ports;
        this.nThreads = nThreads;
        this.cm = cm;
        //System.out.println("ThreadPoolListener started.");
    }

    @Override
    public void run() {
        pool = null;
        socket = null;
        while (socket == null) {
            try {
                socket = new ServerSocket(port);
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
        //System.out.println("ThreadPoolListener is running.");
        while (flag) {
            try {
                pool.execute(new ConnectionListener(socket.accept(), cm));
            } catch (Exception e) {
                if (!flag) break;
                System.out.println("Failed to add new ConnectionListener");
            }
        }
        //System.out.println("ThreadPoolListener end");

    }
    public void start(){
        if (t == null) {
            t = new Thread (this, "ThreadPoolListener_Thread");
            t.start ();
        }
    }
    public void stop() {
        flag = false;
        try{
            socket.close();
        }catch(Exception e){}
        pool.shutdown();
        t.interrupt();
    }
}

//thread with pool speakers
class ThreadPoolSpeaker implements Runnable{
    protected Thread t;
    private boolean flag = true;
    private int port, nThreads;
    private ServerSocket socket;
    private ExecutorService pool;
    private ChatServer cm;

    ThreadPoolSpeaker(int ports, int nThreads, ChatServer cm) {
        this.port = ports;
        this.nThreads = nThreads;
        this.cm = cm;
        //System.out.println("ThreadPoolSpeaker started.");
    }

    @Override
    public void run() {
        pool = null;
        socket = null;
        while (socket == null) {
            try {
                socket = new ServerSocket(port);
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
        //System.out.println("ThreadPoolSpeaker is running.");
        while (flag) {
            try {
                pool.execute(new ConnectionSpeaker(socket.accept(), cm));
            } catch (Exception e) {
                if (!flag) break;
                System.out.println("Failed to add new ConnectionSpeaker");
            }
        }
        //System.out.println("ThreadPoolSpeaker end");

    }
    public void start(){
        if (t == null) {
            t = new Thread (this, "ThreadPoolSpeaker_Thread");
            t.start ();
        }
    }
    public void stop() {
        flag = false;
        try{
            socket.close();
        }catch(Exception e){}
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
    private ChatServer cm;

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
//ConnectionSpeaker
//sends msg to client
class ConnectionSpeaker implements Runnable{
    private Thread t;
    private Socket socket;
    private boolean loop = true;
    private PrintWriter out;
    private ChatServer cm;
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
            if(cm != null){
                ArrayList<String> msgs = cm.send();
                if(n != msgs.size()){
                    //is new message
                    if(n > msgs.size()){
                        //msgs had been cleared
                        n = 0;
                    }
                    for(int i = n; i < msgs.size(); i++){
                        speak(msgs.get(i));
                    }
                    n = msgs.size();
                }
            }
            else {
                this.stop();
            }
            try{
                Thread.sleep(1000);
            }catch(Exception e){
                break;
            }
        }
        //System.out.println("ConnectionSpeaker stopped "+t.getId());
    }
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