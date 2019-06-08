/*
package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ChatController {
    private static int listenerPort = 55557;
    private static int speakerPort = 55556;
    private static String ip = "127.0.0.1";
    private Socket socket;
    private PrintWriter out;

    private Listener listener;
    //private Speaker speaker;
    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button buttonSend;

    @FXML
    private TextArea chatText;

    @FXML
    private TextField msgText;

    @FXML
    void sendMsg(ActionEvent event) {
        event.consume();
        String m = msgText.getText();
        msgText.setText("");
        //speaker.sendMsg(msg);
        String msg = "username:time>"+m;
        out.println(msg);
        //System.out.println(" ... (sent msg:"+msg+")");
    }

    @FXML
    void displayMsg(String m){
        chatText.setText(chatText.getText()+"\n"+m);
    }

    @FXML
    void initialize() {
        assert buttonSend != null : "fx:id=\"buttonSend\" was not injected: check your FXML file 'chat_window.fxml'.";
        assert chatText != null : "fx:id=\"chatText\" was not injected: check your FXML file 'chat_window.fxml'.";
        assert msgText != null : "fx:id=\"msgText\" was not injected: check your FXML file 'chat_window.fxml'.";

        listener = new Listener(ip, listenerPort, this);
        listener.start();


        //this instead of speaker thread
        try {
            socket = new Socket(ip, speakerPort);
        } catch (Exception e) {
            System.out.println("Listener> Can't create socket.");
        }
        out = null;
        if(socket != null){
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("Listener> Failed to get output stream from server");
            }
        }

        //speaker = new Speaker(ip, speakerPort);
        //speaker.start();

    }

    public void shutdown(){
        listener.stop();
        //speaker.stop();
        try{
            socket.close();
        }catch(Exception e){}

    }
}
*/