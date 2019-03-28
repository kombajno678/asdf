import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class zad8_server {
    public static void main(String[] args) throws Exception{

        try(var listener = new ServerSocket(55555)){
            System.out.println("server is running ...");
            var pool = Executors.newFixedThreadPool(20);
            while(true){
                pool.execute(new Notification(listener.accept()));
            }
        }

    }

    private static class Notification implements Runnable{

        private Socket socket;

        Notification(Socket s){
            socket = s;
        }

        @Override
        public void run(){
            System.out.println("connected: " + socket);

            try{
                var in = new Scanner(socket.getInputStream());
                var out = new PrintWriter(socket.getOutputStream(), true);
                while(in.hasNextLine()){
                    out.println(in.nextLine().toUpperCase());
                }
            }catch(Exception e){
                System.out.println("error " + socket);
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
