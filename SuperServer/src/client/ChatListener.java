package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/*
public class ChatClient extends Application  {
    private static int listenerPort = 55557;
    private static int speakerPort = 55556;
    private static String ip = "127.0.0.1";

    @Override
    public void start(Stage primaryStage) throws Exception{

        FXMLLoader loader = new FXMLLoader(getClass().getResource("chat_window.fxml"));
        Parent root = loader.load();
        ChatController controller = loader.getController();
        primaryStage.setTitle("Chat");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            Alert alert = new Alert(Alert.AlertType.WARNING, "Do wou want to close?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                controller.shutdown();
                Platform.exit();
            }
        });
        primaryStage.show();

    }


    public static void main(String[] args){
        //todo : get ip from args
        launch(args);
    }
}
*/


class ChatListener implements Runnable{
    private Thread t;
    private boolean loop;
    private Socket socket;
    private String ip;
    private int port;
    private Scanner in;
    private Controller cc;

    public ChatListener(String ip, int port, Controller cc) {
        this.ip = ip;
        this.port = port;
        this.cc = cc;
        loop = true;
    }

    @Override
    public void run() {
        System.out.println("ChatListener start");
        if(socket == null)createSocket();
        while(loop){
            try{
                if(in.hasNextLine()){
                    cc.displayMsg(in.nextLine());
                }
            }catch(Exception e){}

        }
        System.out.println("ChatListener end");

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
        try {
            //in.close();
            socket.close();
        }catch(Exception e){}
        //in.close();

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
