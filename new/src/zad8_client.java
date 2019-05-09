import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.DateTimeException;
import java.util.Calendar;
import java.util.Scanner;
import java.net.ConnectException;
class DateStringException extends Exception{
    DateStringException(String msg){
        super(msg);
    }
}
class ServerListenerClass implements Runnable {
    private Thread t;
    Socket server;
    ServerListenerClass() {
        //System.out.println("Creating ");
    }

    public void run() {
        //System.out.println("Running ");
        try {
            var in = new Scanner(server.getInputStream());
            while (true) {
                if(in.hasNextLine()) {
                    String temp = in.nextLine();
                    if (temp.matches("!exit")) break;
                    System.out.println("server> " + temp);
                }
            }
        }catch(Exception e){
            System.out.println("ServerListener> IOException occured: " + e.getMessage());
        }
        //System.out.println("Thread exiting.");
    }

    public void start (Socket s) {
        //System.out.println("Starting ");
        server = s;
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }

}

public class zad8_client{
    static String validateDateString(String date) throws DateStringException{
        String pattern = "20\\d\\d[-.\\/\\\\ ][01]\\d[-.\\/\\\\ ][0-3]\\d[ ][0-2]\\d[:][0-5]\\d";
        Boolean throwFlag = false;
        String msg = "DateStringException: ";
        if(date.matches(pattern)) {
            int year = Integer.parseInt(date.substring(0,4));
            int month = Integer.parseInt(date.substring(5,7));
            int day = Integer.parseInt(date.substring(8,10));
            int hour = Integer.parseInt(date.substring(11,13));
            int minute = Integer.parseInt(date.substring(14,16));
            if(year < 2019){
                throwFlag = true;
                msg += "invalid year, ";
            }
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
            //TODO: leap years
            if(month == 2 && day > 28){
                throwFlag = true;
                msg += "invalid day, ";
            }

            if(hour < 0 || hour > 23){
                throwFlag = true;
                msg += "invalid hour, ";
            }
            if(minute < 0 || minute > 59){
                throwFlag = true;
                msg += "invalid minute, ";
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
            if(hourNow > hour || (hourNow == hour && minuteNow > minute)){
                throwFlag = true;
                msg += "wrong time, ";
            }
            if(month > 9 && day > 9) {
                date = year + "-" + month + "-" + day + " " + date;
            }else
            if(month < 10 && day > 9){
                date = year + "-0" + month + "-" + day + " " + date;
            }else
            if(month > 9){// && day < 10){
                date = year + "-" + month + "-0" + day + " " + date;
            }else
            //if(month < 10 && day < 10)
            {
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

    public static void main(String[] args) throws Exception {
        String ip="", msg="", datetime = "";

        final int port = 55555;
        var scanner = new Scanner(System.in);
        //set servers ip address
        if (args.length < 1) {
            System.err.println("Enter server's ip address:");
            if (scanner.hasNextLine()) {
                ip = scanner.nextLine();
            }
        }else {
            ip = args[0];
        }
        //set msg and datetime from arguments
        if(args.length == 3){
            msg = args[1];
            datetime = args[2];
        }

        Socket socket = null;
        do{
            try{
                socket = new Socket(ip, port);
            }catch(ConnectException e){
                System.out.println("ConnectException: Failed to connect with server");
                Thread.sleep(1000);
            }
        }while(socket == null);

        ServerListenerClass serverListener = new ServerListenerClass();
        serverListener.start(socket);
        var out = new PrintWriter(socket.getOutputStream(), true);
        Boolean newNotificationFlag = true;
        while(newNotificationFlag) {//main loop
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
            out.println(msg);
            out.println(datetime);
            msg = "";
            datetime = "";
            System.out.println("Waiting for notifications, " +
                    "\n - \"new\" to create another one " +
                    "\n - \"exit\" to exit");
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
        }catch(IOException e){
            //caught
            System.out.println("failed to close socket, "+e.getMessage());
        }
    }
}