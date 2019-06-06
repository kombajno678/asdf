package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static int listenerPort = 55557;
    private static int speakerPort = 55556;
    private static String ip = "127.0.0.1";
    public static void main(String[] args){

        Listener listener = new Listener(ip, listenerPort);
        listener.start();
        Speaker speaker = new Speaker(ip, speakerPort);
        speaker.start();
        try {
            Thread.sleep(99999999);
        }catch (Exception e){}

    }

}


class Listener implements Runnable{
    private Thread t;
    private boolean loop;
    private Socket socket;
    private String ip;
    private int port;
    private Scanner in;

    public Listener(String ip, int port) {
        this.ip = ip;
        this.port = port;
        loop = true;
    }

    @Override
    public void run() {
        if(socket == null)createSocket();
        while(loop){
            if(in.hasNextLine()){
                System.out.println("server > "+in.nextLine());
            }
        }
    }
    public void start(){
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }
    public void stop(){
        loop = false;
        t.interrupt();
    }
    private void createSocket(){
        try {
            socket = new Socket(ip, port);
        } catch (Exception e) {
            System.out.println("Listener> Can't create socket.");
        }
        in = null;
        if(socket != null){
            try {
                in = new Scanner(socket.getInputStream());
            } catch (IOException e) {
                System.out.println("Listener> Failed to get output stream from server");
            }
        }
    }
}


class Speaker implements Runnable{
    private Thread t;
    private boolean loop;
    private Socket socket;
    private String ip;
    private int port;
    private PrintWriter out;

    public Speaker(String ip, int port) {
        loop = true;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void run() {
        if(socket == null)createSocket();
        Scanner in = new Scanner(System.in);
        while(loop){
            if(in.hasNextLine()){
                String msg = "username:time>"+in.nextLine();
                out.println(msg);
                System.out.println(" ... (sent msg:"+msg+")");
            }
        }
    }
    public void start(){
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }
    public void stop(){
        loop = false;
        t.interrupt();
    }
    private void createSocket(){
        try {
            socket = new Socket(ip, port);
        } catch (Exception e) {
            System.out.println("Listener> Can't create socket.");
        }
        out = null;
        if(socket != null){
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("Listener> Failed to get output stream from server");
            }
        }
    }
}