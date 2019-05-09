import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.util.*;
class Notificationn{
    Calendar date = Calendar.getInstance();
    String text;
    Notificationn(String d, String t){
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

public class zad8_server_nnn {
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
            Boolean loopFlag = true;
            BufferedReader in = null;
            PrintWriter out = null;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            }catch(IOException e){
                System.out.println(socket.getPort() +"> IOException occured");
                out.println("IOException occured");
                loopFlag = false;
            }
            List<Notificationn> notificationList = new ArrayList<Notificationn>();
            while(loopFlag) {
                try{
                    while(!notificationList.isEmpty()){//waiting loop, checking if can send notifications
                        Iterator<Notificationn> iterator = notificationList.listIterator();
                        while (iterator.hasNext()) {
                            Notificationn temp = iterator.next();
                            if(temp.date.getTimeInMillis() - Calendar.getInstance().getTimeInMillis() <= 0){
                                    //time to send notification
                                    out.println(temp.text);
                                    notificationList.remove(temp);
                                }
                        }
                        //try{
                        //if(in.hasNext()){
                            //in.hasNext("@new")
                            String temp = in.readLine();

                            System.out.println("temp:"+temp);
                            if(temp.length() == 0)continue;;
                            if(temp.matches("@exit")){
                                //exit
                                try{
                                    socket.close();
                                }catch(IOException e){
                                    //caught
                                    System.out.println(socket.getPort() +"> "+e.getMessage());
                                }finally{
                                    System.out.println(socket.getPort() +"> closed");
                                }
                                return;
                            }
                            if(temp.matches("@new")){
                                //new notification
                                break;
                            }
                        //}
                        //}catch(Exception e){}
                    }
                    //if(in.hasNextLine()){
                        String temp = in.readLine();
                        if(!temp.matches("@new")){
                            continue;
                        }
                    //}
                    System.out.println(socket.getPort() + "> waiting for info ");
                    String text = "", date = "";
                    //get text
                    //if(in.hasNextLine()){
                        text = in.readLine();
                    //}
                    //get date
                    //if(in.hasNextLine()){
                        date = in.readLine();
                    //}
                    //create notification
                    //Notification n = new Notification(date, text);
                    notificationList.add(new Notificationn(date, text));
                    System.out.println(socket.getPort() + "> notification added: " + text + " @ " + date);

                    //wait till datetime of notification
                    //long waitTimeSeconds = (n.date.getTimeInMillis() - Calendar.getInstance().getTimeInMillis())/1000;
                    //System.out.println(socket.getPort() + "> going to sleep for "+waitTimeSeconds+"s");
                    //Thread.sleep(waitTimeSeconds*1000);

                    //send text from notification
                    //out.println(n.text);
                    //System.out.println(socket.getPort() + "> notification text sent");

                    //close connection
                }catch(Exception e){
                    System.out.println(socket.getPort() +"> error " + socket);
                    System.out.println(socket.getPort() +"> "+e.getMessage());
                }
            }

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
