import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class zad8_client {
    static Boolean validateDateString(String date){
        String pattern = "20\\d\\d[-.\\/\\\\ ][01]\\d[-.\\/\\\\ ][0-3]\\d[ ][0-2]\\d[:][0-5]\\d";
        if(date.matches(pattern)) {
            int year = Integer.parseInt(date.substring(0,4));
            int month = Integer.parseInt(date.substring(5,7));
            int day = Integer.parseInt(date.substring(8,10));
            int hour = Integer.parseInt(date.substring(11,13));
            int minute = Integer.parseInt(date.substring(14,16));
            if(year < 2019)return false;
            if(month < 1 || month > 12)return false;
            if(day < 1 || day > 31)return false;
            if(hour < 0 || hour > 23)return false;
            if(minute < 0 || minute > 59)return false;
            if((month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12)
                    && day > 31 )return false;
            if((month == 4 || month == 6 || month == 9 || month == 11)&& day > 30) return false;
            //TODO: leap years
            if(month == 2 && day > 28)return false;
            return true;
        }
        return false;
    }
    public static void main(String[] args) throws Exception {
        String ip="", msg="", datetime = "";
        final int port = 55555;
        var scanner = new Scanner(System.in);
        //set servers ip address
        if (args.length < 1) {
            System.err.println("input servers ip address:");
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
        try (var socket = new Socket(ip, port)) {
            System.out.println("Enter lines of text then Ctrl+D or Ctrl+C to quit");

            var in = new Scanner(socket.getInputStream());
            var out = new PrintWriter(socket.getOutputStream(), true);


            if(msg.isEmpty()) {
                System.out.println("input text of notification:");
                if (scanner.hasNextLine()) {
                    msg = scanner.nextLine();
                }
            }
            out.println(msg);

            if(datetime.isEmpty()){
                System.out.println("input date and time of notification: (format: \"yyyy-mm-dd- hh:mm\")");
                if(scanner.hasNextLine()){
                    datetime = scanner.nextLine();
                }
            }
            if(validateDateString(datetime)){
                out.println(datetime);
            }else{
                //throw exception or sth
                //abort
            }


            while(true){
                if (in.hasNextLine()) {
                    System.out.println("server> "+in.nextLine());
                }
                if (scanner.hasNextLine()) {
                    String next = scanner.nextLine();
                    if (next.matches("exit")){
                        break;
                    }
                    out.println(scanner.nextLine());
                }
            }
        }
    }
}