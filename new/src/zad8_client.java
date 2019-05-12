import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Year;
import java.util.Calendar;
import java.util.Scanner;
import java.util.concurrent.Executors;

class DateStringException extends Exception{
    DateStringException(String msg){
        super(msg);
    }
}
class ServerListenerClass implements Runnable {
    private Thread t;
    Socket server;
    ServerListenerClass(Socket s) {
        //System.out.println("Creating ");
        server = s;
        this.start();
    }

    public void run() {
        try {
            var in = new Scanner(server.getInputStream());
            while (true) {
                if(in.hasNextLine()) {
                    String temp = in.nextLine();
                    if (temp.matches("!exit")){
                        System.out.println("server> !exit");
                        break;
                    }
                    System.out.println("server> " + temp);
                }
            }
        }catch(Exception e){
            System.out.println("ServerListener> IOException occured: " + e.getMessage());
        }
        System.out.println("ServerListener closed");
    }

    public void start () {
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }

}

public class zad8_client{
    static String validateDateString(String date) throws DateStringException{
        String pattern = "2\\d\\d\\d[-.\\/\\\\ ][01]\\d[-.\\/\\\\ ][0-3]\\d[ ][0-2]\\d[:][0-5]\\d";
        boolean throwFlag = false;
        String msg = "Error while validating date/time: ";
        if(date.matches(pattern)) {
            int year = Integer.parseInt(date.substring(0,4));
            int month = Integer.parseInt(date.substring(5,7));
            int day = Integer.parseInt(date.substring(8,10));
            int hour = Integer.parseInt(date.substring(11,13));
            int minute = Integer.parseInt(date.substring(14,16));
            if(month < 1 || month > 12){
                throwFlag = true;
                msg += "invalid month, ";
            }
            if(day < 1 ){
                throwFlag = true;
                msg += "invalid day, ";
            }
            if((month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12)
                    && day > 31 ){
                throwFlag = true;
                msg += "invalid day, ";
            }
            if((month == 4 || month == 6 || month == 9 || month == 11)&& day > 30) {
                throwFlag = true;
                msg += "invalid day, ";
            }
            if(Year.isLeap(year)){
                if(month == 2 && day > 29){
                    throwFlag = true;
                    msg += "invalid day, ";
                }
            }else{
                if(month == 2 && day > 28){
                    throwFlag = true;
                    msg += "invalid day, ";
                }
            }
            if(hour < 0 || hour > 23){
                throwFlag = true;
                msg += "invalid hour, ";
            }
            if(minute < 0 || minute > 59){
                throwFlag = true;
                msg += "invalid minute, ";
            }
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month-1, day, hour, minute, 0);
            long waitTime = (calendar.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
            if(waitTime <= 0){
                throwFlag = true;
                msg += "date/time must be in future, ";
            }

        }else if(date.matches("\\d\\d:\\d\\d")){//user entered only time
            int hour = Integer.parseInt(date.substring(0,2));
            int minute = Integer.parseInt(date.substring(3,5));
            if(hour < 0 || hour > 23){
                throwFlag = true;
                msg += "invalid hour, ";
            }
            if(minute < 0 || minute > 59){
                throwFlag = true;
                msg += "invalid minute, ";
            }
            //add today's date
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH)+1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int hourNow = calendar.get(Calendar.HOUR_OF_DAY);
            int minuteNow = calendar.get(Calendar.MINUTE);
            if(hourNow > hour || (hourNow == hour && minuteNow >= minute)){
                throwFlag = true;
                msg += "time must be in future, ";
            }
            if(month > 9 && day > 9) {
                date = year + "-" + month + "-" + day + " " + date;
            }else if(month < 10 && day > 9){
                date = year + "-0" + month + "-" + day + " " + date;
            }else if(month > 9){
                date = year + "-" + month + "-0" + day + " " + date;
            }else{
                date = year + "-0" + month + "-0" + day + " " + date;
            }
        }else{
            throwFlag = true;
            msg += "wrong format";
        }
        if(throwFlag){
            throw new DateStringException(msg);
        }else{
            return date;
        }
    }

    public static void main(String[] args) {
        String ip="", msg="", datetime = "";
        boolean newNotificationFlag = true;
        final int port = 55555;
        var scanner = new Scanner(System.in);
        //---------------------------------------------set servers ip address
        if (args.length < 1) {
            System.err.println("Enter server's ip address:");
            if (scanner.hasNextLine()) {
                ip = scanner.nextLine();
            }
        }else {
            ip = args[0];
        }
        //---------------------------------------------set msg and datetime from arguments
        if(args.length == 3){
            msg = args[1];
            datetime = args[2];
        }
        //---------------------------------------------creating socket
        Socket socket = null;
        do{
            try{
                socket = new Socket(ip, port);
            }catch(Exception e){
                System.out.println("Can't connect to server. Trying again in 1s");
                try{
                    Thread.sleep(1000);
                }catch(InterruptedException ie){
                    //System.out.println("InterruptedException: thread interrupted while sleeping");
                }
            }
        }while(socket == null);
        //----------------------------------------------creating server listener thread
        var pool = Executors.newFixedThreadPool(1);
        pool.execute(new ServerListenerClass(socket));
        //ServerListenerClass serverListener = new ServerListenerClass();
        //serverListener.start(socket);
        PrintWriter out = null;
        try{
            out = new PrintWriter(socket.getOutputStream(), true);
        }catch (IOException e){
            System.out.println("IOException: failed to get output stream from server");
            newNotificationFlag = false;
        }

        while(newNotificationFlag) {//----------------------------------------main loop---------------------------------
            if (msg.isEmpty()) {
                System.out.println("Enter text of notification:");
                if (scanner.hasNextLine()) {
                    msg = scanner.nextLine();
                }
            }
            while (true) {
                if (datetime.isEmpty()) {
                    System.out.println("Enter date and time of notification(\"YYYY-MM-DD hh:mm\" or \"hh:mm\"):");
                    if (scanner.hasNextLine()) {
                        datetime = scanner.nextLine();
                    }
                }
                try {
                    datetime = validateDateString(datetime);
                    break;
                }catch(DateStringException e){
                    System.out.println(e.getMessage());
                    datetime = "";
                }
            }
            //---------------------sending data to server
            out.println(msg);
            out.println(datetime);
            msg = "";
            datetime = "";
            System.out.println("Waiting for notifications, " +
                    "\n - \"new\" to create another one " +
                    "\n - \"exit\" to exit");
            //---------------------waiting
            while (true) {
                if (scanner.hasNextLine()) {
                    String next = scanner.nextLine();
                    if (next.matches("new")) {
                        out.println("!new");
                        break;
                    }
                    if (next.matches("exit")) {
                        newNotificationFlag = false;
                        out.println("!exit");
                        break;
                    }
                }
            }
        }
        System.out.println("Exiting program...");
        try{
            socket.close();
            pool.shutdownNow();
        }catch(IOException e){
            System.out.println("Failed to close socket");
        }
        return;
    }
}