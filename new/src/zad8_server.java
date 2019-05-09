import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.*;
class NotificationSenderClass implements Runnable {
    private Thread t;
    private String threadName;
    Socket client;
    Notification n;
    NotificationSenderClass( String name) {
        threadName = name;
    }

    public void run() {
        System.out.println(client.getPort() + "> Running " +  threadName );
        long waitTimeSeconds = (n.date.getTimeInMillis() - Calendar.getInstance().getTimeInMillis())/1000;
        try {
            var out = new PrintWriter(client.getOutputStream(), true);
            Thread.sleep(waitTimeSeconds*1000);
            out.println("*beep* *boop*, notification:"+n.text);
            System.out.println(client.getPort() + "> notification text sent");

        } catch (Exception e) {
            System.out.println(client.getPort() + "> Thread " +  threadName + " " + e.getMessage());
        }
        System.out.println(client.getPort() + "> Thread " +  threadName + " exiting.");
    }

    public void start (Socket s, Notification n) {
        client = s;
        this.n = n;
        //sleepTime = sleep;
        System.out.println(client.getPort() + ">Starting " +  threadName );
        if (t == null) {
            t = new Thread (this, threadName);
            t.start ();
        }
    }
}
class Notification{
    Calendar date = Calendar.getInstance();
    String text;
    Notification(String d, String t){
        //"yyyy?MM?dd?hh?mm"
        int year = Integer.parseInt(d.substring(0,4));
        int month = Integer.parseInt(d.substring(5,7));
        int day = Integer.parseInt(d.substring(8,10));
        int hour = Integer.parseInt(d.substring(11,13));
        int minute = Integer.parseInt(d.substring(14,16));
        //System.out.println("... y" + year+ " m" + month+ " d" + day+ " h" + hour+ " m" + minute + " ...");
        date.set(year, month-1, day, hour, minute, 0);
        text = t;
    }
    @Override public String toString(){
        return "\""+text+"\"" + " @ " + date.getTime().toString();
    }
}

public class zad8_server {
    public static void main(String[] args) throws Exception{
        try(var listener = new ServerSocket(55555)){
            System.out.println("hello, notification server is up and running ...");
            var pool = Executors.newFixedThreadPool(20);
            while(true){
                pool.execute(new Connection(listener.accept()));
            }
        }
    }
    private static class Connection implements Runnable{
        private Socket socket;
        Connection(Socket s){
            socket = s;
        }
        @Override
        public void run(){
            System.out.println(socket.getPort() +"> connected: " + socket);
            try {
                var in = new Scanner(socket.getInputStream());
                var out = new PrintWriter(socket.getOutputStream(), true);
                Boolean loopFlag = true;
                //out.println("You are now connected to server");
                int notificationCounter = 1;
                while(loopFlag){//main loop
                    System.out.println(socket.getPort() + "> creates new notification...");
                    String text = "", date = "";
                    Notification n = null;
                    try {//get text
                        if (in.hasNextLine()) {
                            text = in.nextLine();
                        }
                        //get date
                        if (in.hasNextLine()) {
                            date = in.nextLine();
                        }
                        //create notification
                        n = new Notification(date, text);
                    }catch(Exception e){
                        System.out.println(socket.getPort() + "> client disconnected: ");
                        break;
                    }
                    System.out.println(socket.getPort() + "> notification created: " + n);
                    NotificationSenderClass notificationSender = new NotificationSenderClass("not#"+notificationCounter);
                    notificationCounter += 1;
                    notificationSender.start(socket, n);
                    System.out.println(socket.getPort() + "> waiting...");
                    while (true) {
                        try {
                            String temp = in.nextLine();
                            if (temp.matches("!new")) {
                                break;
                            }
                            if (temp.matches("!exit")) {
                                out.println("!exit");//closing server listener on clients side
                                //loopFlag = false;
                                break;
                            }
                        }catch(Exception e){
                            System.out.println(socket.getPort() + "> client disconnected ");
                            //loopFlag = false;
                            break;
                        }
                    }
                }
            }catch(Exception e){
                System.out.println(socket.getPort() +"> error " + socket);
                System.out.println(socket.getPort() +"> "+e.getMessage());
                e.printStackTrace();
            } finally{
                try{
                    socket.close();
                }catch(IOException e){
                    System.out.println(socket.getPort() +"> "+e.getMessage());
                }finally{
                    System.out.println(socket.getPort() +"> closed");
                }
            }
        }
    }
}
