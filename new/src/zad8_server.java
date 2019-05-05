import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.*;
class Notification{

    Calendar date = Calendar.getInstance();
    String text;

    Notification(String d, String t){
        //"yyyy-mm-dd hh:mm"
        Integer year = Integer.parseInt(d.substring(0,3));
        Integer month = Integer.parseInt(d.substring(4,5));
        Integer day = Integer.parseInt(d.substring(6,7));
        Integer hour = Integer.parseInt(d.substring(8,9));
        Integer minute = Integer.parseInt(d.substring(10,11));
        date.set(year, month, day, hour, minute);
        text = t;
    }
    @Override public String toString(){
        return text + " at: " + date.toString();
    }

}

public class zad8_server {
    public static void main(String[] args) throws Exception{
        try(var listener = new ServerSocket(55555)){
            System.out.println("server is running ...");
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
            System.out.println("connected: " + socket);
            try{
                var in = new Scanner(socket.getInputStream());
                var out = new PrintWriter(socket.getOutputStream(), true);
                while(in.hasNextLine()){
                    //out.println(in.nextLine().toUpperCase());
                    //get text
                    String text = in.nextLine();
                    String date = in.nextLine();
                    Notification n = new Notification(date, text);
                    out.println(n);
                    //wait till date
                    //send text
                    //close

                }
            }catch(Exception e){
                System.out.println("error " + socket);
                System.out.println(e.getMessage());
            } finally{
                try{
                    socket.close();
                }catch(IOException e){
                    //caught
                }finally{
                    System.out.println("closed " + socket);
                }
            }


        }


    }

}
