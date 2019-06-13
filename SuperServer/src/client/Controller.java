package client;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
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
    private static int listenerPort;// = 55557;
    private static int speakerPort;// = 55556;
    private ChatListener listener;
    private ChatSpeaker speaker;

    //private ClientThread.UpdateFilesFromServerClass updater = null;
    //private ClientThread.CheckForNewLocalFiles checker = null;

    private ClientThread.BackgroundTasks bg = null;




    public Controller(){}

    @FXML
    private void initialize(){
        buttonConnect.setDisable(false);
        inputUsername.setDisable(false);
        inputPath.setDisable(false);
        inputIP.setDisable(false);
        inputPort.setDisable(false);
        inputUsername.setDisable(false);
        centerBox.setDisable(true);
        buttonDisconnect.setDisable(true);
        columnNames.setCellValueFactory(new PropertyValueFactory<>("filename"));
        columnSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        columnOwner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        columnShared.setCellValueFactory(new PropertyValueFactory<>("others"));
        columnStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        //buttons share, unshare, delete are disabled when list is not focused
        tableFiles.focusedProperty().addListener((obs, oldVal, newVal) ->{
                if(newVal){
                    //sth is selected
                    buttonShare.setDisable(false);
                    buttonUnshare.setDisable(false);
                    buttonDelete.setDisable(false);
                }else{
                    //nothing is selected
                    buttonShare.setDisable(true);
                    buttonUnshare.setDisable(true);
                    buttonDelete.setDisable(true);
                }
        });

        //send button is disabled when no test in msg field is entered
        textMsg.textProperty().addListener((observable, oldValue, newValue) ->{
            if(textMsg.getText().length() < 1){
                buttonSend.setDisable(true);
            }else{
                buttonSend.setDisable(false);
            }
        });

    }

    @FXML
    private VBox formBox;
    @FXML
    private VBox centerBox;
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

    @FXML void setTextLeft(String text){
        Platform.runLater(() -> textBotLeft.setText(text));
    }
    @FXML void addTextLeft(String text){
        Platform.runLater(()->textBotLeft.setText(textBotLeft.getText()+text));
    }

    @FXML void dialog(String title, String text){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
    @FXML void delete(ActionEvent event){
        event.consume();
        FileEntry file = tableFiles.getItems().get(tableFiles.getFocusModel().getFocusedCell().getRow());
        //file : selected file on list
        //check if user is owner
        if(file.getOwner().equals(username)){
            //yes/no dialog
            Alert alert = new Alert(Alert.AlertType.WARNING, "Do wou want to delete \""+ file.getFilename()+"\" ?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                bg.delete(file);
            }
        }else{
            dialog("SuperClient - info","You can't delete file that's not yours.");
        }

    }
    @FXML
    void sendMsg(ActionEvent event) {
        event.consume();
        speaker.sendMsg(username+"> "+textMsg.getText());
        textMsg.setText("");
    }

    @FXML
    synchronized void displayMsg(String m){
        textChat.appendText(m+"\n");
    }

    public void connect() {
        //disable login form
        inputUsername.setDisable(true);
        inputPath.setDisable(true);
        inputIP.setDisable(true);
        inputPort.setDisable(true);
        inputUsername.setDisable(true);
        buttonConnect.setDisable(true);
        boolean validFlag;
        try {
            username = inputUsername.getText();
            localFolder = inputPath.getText();
            ip = inputIP.getText();
            port = Integer.parseInt(inputPort.getText());
            speakerPort = port + 1;
            listenerPort = port + 2;
            validFlag = true;
        } catch (Exception e) {
            validFlag = false;
        }
        if(validFlag){
            clearFiles();
            bg = new ClientThread.BackgroundTasks(localFolder, username, ip, port, this);

            listener = new ChatListener(ip, listenerPort, this);
            speaker = new ChatSpeaker(ip, speakerPort, this);
            listener.start();
            speaker.start();
            //enable all else
            centerBox.setDisable(false);
            tableFiles.setDisable(false);
            buttonDisconnect.setDisable(false);
            buttonClear.setDisable(false);
            textConsole.setDisable(false);
            textConsole.appendText("Connecting to server ("+ip+":"+port+") ... \n");
            textChat.clear();

        }else{
            //enable login form
            inputUsername.setDisable(false);
            inputPath.setDisable(false);
            inputIP.setDisable(false);
            inputPort.setDisable(false);
            inputUsername.setDisable(false);
            buttonConnect.setDisable(false);
            //error dialog
            dialog("SuperClient - error", "Something went wrong. Check if entered values are correct.");
        }

    }
    public void disconnect() {
        bg.stop();
        listener.stop();
        speaker.stop();
        buttonConnect.setDisable(false);
        inputUsername.setDisable(false);
        inputPath.setDisable(false);
        inputIP.setDisable(false);
        inputPort.setDisable(false);
        inputUsername.setDisable(false);
        centerBox.setDisable(true);
        tableFiles.setDisable(true);
        buttonDisconnect.setDisable(true);
        buttonClear.setDisable(true);
        buttonShare.setDisable(true);
        buttonDelete.setDisable(true);
        buttonUnshare.setDisable(true);
        textConsole.setDisable(true);
    }
    public void shutdown(){
        if(bg != null)bg.stop();
        if(listener != null)listener.stop();
        if(speaker != null)speaker.stop();
    }
    public void share(){
        FileEntry file = tableFiles.getItems().get(tableFiles.getFocusModel().getFocusedCell().getRow());
        //file : selected file on list
        Platform.runLater(() -> printText("sharing : " + file));
        if(file.getOwner().equals(username)){
            bg.share(file);
        }
    }
    public void unshare(){
        FileEntry file = tableFiles.getItems().get(tableFiles.getFocusModel().getFocusedCell().getRow());
        //file : selected file on list
        if(file.getOthers().size() > 0){
            Platform.runLater(() -> printText("unsharing : " + file));
            if(file.getOwner().equals(username)){
                bg.unshare(file);
            }
        }

    }
    public void clear(){
        textConsole.clear();
    }
    public synchronized void printText(String a){
        try {
            textConsole.appendText(a + "\n");
        }catch(Exception e){
            System.out.println("printText Exception : " + e.getMessage());
        }
    }
    public void clearFiles(){
        tableFiles.getItems().clear();
    }
    public void updateFiles(ArrayList<FileEntry> f) {
        ObservableList<FileEntry> listForGui = FXCollections.observableArrayList(f);
        if(tableFiles.getItems().size() > 0){
            for(int iGui = 0; iGui < tableFiles.getItems().size(); iGui++){
                FileEntry fold = tableFiles.getItems().get(iGui);
                boolean found = false;
                for(Iterator<FileEntry> i = listForGui.iterator(); i.hasNext();){
                    FileEntry fnew = i.next();
                    if(fnew.getFilename().equals(fold.getFilename()) && fnew.getOwner().equals(fold.getOwner()) ){
                        //same file entry
                        //update info
                        fold.setOthers(fnew.getOthers());
                        if(fold.getSize() != fnew.getSize()){
                            fold.setSize(fnew.getSize());
                        }
                        //later delete from list4gui
                        found = true;

                        i.remove();
                        break;
                    }else{
                        //continue search
                    }
                }
                if(!found){
                    tableFiles.getItems().remove(iGui);
                    //fGui.remove();
                }else{
                    tableFiles.getItems().set(iGui, fold);
                }
            }

        }

        if(listForGui.size() > 0)
            for(FileEntry fnew : listForGui)
                tableFiles.getItems().addAll(fnew);

        Platform.runLater(() -> labelFiles.setText(""+tableFiles.getItems().size()));
    }

}

