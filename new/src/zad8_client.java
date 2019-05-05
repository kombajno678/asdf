import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class zad8_client {
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
            out.println(datetime);

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