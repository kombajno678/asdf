import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.*;
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
            try{
                var in = new Scanner(socket.getInputStream());
                var out = new PrintWriter(socket.getOutputStream(), true);
                //out.println("you are now connected to server");

                String text = "", date = "";
                //get text
                if(in.hasNextLine()){
                    text = in.nextLine();
                }
                //get date
                if(in.hasNextLine()){
                    date = in.nextLine();
                }
                //create notification
                Notification n = new Notification(date, text);
                System.out.println(socket.getPort() + "> notification created: " + n);

                //wait till datetime from notification
                long waitTimeSeconds = (n.date.getTimeInMillis() - Calendar.getInstance().getTimeInMillis())/1000;
                System.out.println(socket.getPort() + "> going to sleep for "+waitTimeSeconds+"s");
                Thread.sleep(waitTimeSeconds*1000);

                //send text from notification
                out.println(n.text);
                System.out.println(socket.getPort() + "> notification text sent");

                //close
            }catch(Exception e){
                System.out.println(socket.getPort() +"> error " + socket);
                System.out.println(socket.getPort() +"> "+e.getMessage());
            } finally{
                try{
                    socket.close();
                }catch(IOException e){
                    //caught
                    System.out.println(socket.getPort() +"> "+e.getMessage());
                }finally{
                    System.out.println(socket.getPort() +"> closed");
                }
            }
        }
    }
}
