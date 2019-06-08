package client;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import jdk.nashorn.internal.ir.annotations.Ignore;
import server.FileEntry;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import client.ChatListener;

public class Controller {
    private String username;// = "adamko";
    private String localFolder;// = "local\\"+username;
    private String ip;// = "127.0.0.1";
    private int port;// = 55555;
    private static int listenerPort = 55557;
    private static int speakerPort = 55556;
    //private ClientThread.UpdateFilesFromServerClass updater = null;
    //private ClientThread.CheckForNewLocalFiles checker = null;

    private ClientThread.BackgroundTasks bg = null;

    //chat
    private Socket socket;
    private PrintWriter out;
    private ChatListener listener;


    public Controller(){}

    @FXML
    private void initialize(){
        buttonConnect.setDisable(false);
        inputUsername.setDisable(false);
        inputPath.setDisable(false);
        inputIP.setDisable(false);
        inputPort.setDisable(false);
        inputUsername.setDisable(false);

        tableFiles.setDisable(true);
        buttonDisconnect.setDisable(true);
        buttonClear.setDisable(true);
        textConsole.setDisable(true);

        columnNames.setCellValueFactory(new PropertyValueFactory<>("filename"));
        columnSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        columnOwner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        columnShared.setCellValueFactory(new PropertyValueFactory<>("others"));
        columnStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

    }

    @FXML
    private Label textBotLeft;
    @FXML
    private Label labelFiles;
    @FXML
    private Label textBotRight;
    @FXML
    private Button buttonDelete;
    @FXML
    private Button buttonShare;
    @FXML
    private Button buttonUnshare;
    @FXML
    private TableView<FileEntry> tableFiles;
    @FXML
    private TableColumn<FileEntry, String> columnNames;
    @FXML
    private TableColumn<FileEntry, String> columnSize;
    @FXML
    private TableColumn<FileEntry, String> columnOwner;
    @FXML
    private TableColumn<FileEntry, String> columnShared;
    @FXML
    private TableColumn<FileEntry, String> columnStatus;
    @FXML
    public TextArea textConsole;
    @FXML
    private TextField inputUsername;
    @FXML
    private TextField inputPath;
    @FXML
    private TextField inputIP;
    @FXML
    private TextField inputPort;
    @FXML
    private Button buttonConnect;
    @FXML
    private Button buttonDisconnect;
    @FXML
    private Button buttonClear;
    @FXML
    private Button buttonSend;
    @FXML
    private TextArea textChat;
    @FXML
    private TextField textMsg;

    @FXML
    void sendMsg(ActionEvent event) {
        event.consume();
        String m = textMsg.getText();
        textMsg.setText("");
        //speaker.sendMsg(msg);
        String msg = "username:time>"+m;
        out.println(msg);
        //System.out.println(" ... (sent msg:"+msg+")");
    }

    @FXML
    void displayMsg(String m){
        textChat.setText(textChat.getText()+"\n"+m);
    }
    void createChatSpeaker(){
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
    }
    void closeChatSpeaker(){
        try{
            socket.close();
        }catch(Exception e){}
    }

    public void Connect() {
        boolean validFlag = false;
        try {
            username = inputUsername.getText();
            localFolder = inputPath.getText();
            ip = inputIP.getText();
            port = Integer.parseInt(inputPort.getText());
            validFlag = true;
        } catch (Exception e) {
            validFlag = false;
        }
        if(validFlag){
            clearFiles();
            //login = new ClientThread.Login(ip, port,username);
            createChatSpeaker();
            bg = new ClientThread.BackgroundTasks(localFolder, username, ip, port, this);
            listener = new ChatListener(ip, listenerPort, this);
            listener.start();


            //disable login form
            inputUsername.setDisable(true);
            inputPath.setDisable(true);
            inputIP.setDisable(true);
            inputPort.setDisable(true);
            inputUsername.setDisable(true);
            buttonConnect.setDisable(true);

            //enable all else
            tableFiles.setDisable(false);
            buttonDisconnect.setDisable(false);
            buttonClear.setDisable(false);
            buttonShare.setDisable(false);
            buttonUnshare.setDisable(false);

            textConsole.setDisable(false);
            textConsole.setText("Connecting to " + ip + "... ");

        }

    }
    public void Disconnect() {
        //updater.stop();
        //checker.stop();
        //login.stop();
        bg.stop();
        listener.stop();
        closeChatSpeaker();
        buttonConnect.setDisable(false);
        inputUsername.setDisable(false);
        inputPath.setDisable(false);
        inputIP.setDisable(false);
        inputPort.setDisable(false);
        inputUsername.setDisable(false);

        tableFiles.setDisable(true);
        buttonDisconnect.setDisable(true);
        buttonClear.setDisable(true);
        buttonShare.setDisable(true);
        buttonUnshare.setDisable(true);
        textConsole.setDisable(true);

    }
    public void Share(){
        //TablePosition temp = tableFiles.getFocusModel().getFocusedCell();
        FileEntry file = tableFiles.getItems().get(tableFiles.getFocusModel().getFocusedCell().getRow());
        //file : selected file on list
        Platform.runLater(() -> printText("share : " + file));
        if(file.getOwner().equals(username)){
            bg.share(file);
        }
    }
    public void Unshare(){
        //TablePosition temp = tableFiles.getFocusModel().getFocusedCell();
        FileEntry file = tableFiles.getItems().get(tableFiles.getFocusModel().getFocusedCell().getRow());
        //file : selected file on list
        Platform.runLater(() -> printText("unshare : " + file));
        if(file.getOwner().equals(username)){
            bg.unshare(file);
        }
    }
    public void Clear(){
        textConsole.clear();
    }
    public void printText(String a){

        try {
            Platform.runLater(() -> {
                textConsole.setText(textConsole.getText() + "\n" + a + "\n");
                textConsole.setScrollTop(Double.MAX_VALUE);
            });
        }catch(Exception e){
            out.println("printText Exception : " + e.getMessage());
        }
    }
    public void setFileStatus(String fname ,boolean status){
        String newStatus;
        if(status){
            newStatus = "on server";
        }else{
            newStatus = "ready to upload";
        }
        Iterator<FileEntry> i = tableFiles.getItems().iterator();
        while(i.hasNext()){
            FileEntry temp = i.next();
            if(temp.getFilename().matches(fname)){
                temp.setStatus(newStatus);
                printText("updated status for: " + fname);
                break;
            }
        }
    }
    public void clearFiles(){
        tableFiles.getItems().clear();
    }
    public void updateFiles(ArrayList<FileEntry> f) {
        ObservableList<FileEntry> listForGui = FXCollections.observableArrayList();
        for(FileEntry fe : f){
            listForGui.add(fe);
        }

        if(tableFiles.getItems().size() > 0){
            for(Iterator<FileEntry> fGui = tableFiles.getItems().iterator(); fGui.hasNext();){
                FileEntry fgui = fGui.next();
                boolean found = false;
                for(Iterator<FileEntry> fList = listForGui.iterator(); fList.hasNext();){
                    FileEntry flist = fList.next();
                    if(fgui.equals2(flist)){
                        //same entry, skip, all good
                        //System.out.println(" " + fgui.getFilename() +" equals " + flist.getFilename());
                        //later delete from list4gui
                        found = true;
                        fList.remove();
                        break;
                    }else{
                        //continue search
                    }
                }
                if(!found){
                    fGui.remove();
                }
            }

        }

        if(listForGui.size() > 0)
            for(FileEntry fnew : listForGui)
                tableFiles.getItems().addAll(fnew);

        Platform.runLater(() -> labelFiles.setText(""+tableFiles.getItems().size()));
    }
}

