package client;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class Controller {



    private boolean syncFlag;
    public boolean isSyncFlag() {
        return syncFlag;
    }

    public void setSyncFlag(boolean syncFlag) {
        this.syncFlag = syncFlag;
    }
    private String username;// = "adamko";
    private String localFolder;// = "local\\"+username;
    private String ip;// = "127.0.0.1";
    private int port;// = 55555;
    //private ClientThread.UpdateFilesFromServerClass updater = null;
    //private ClientThread.CheckForNewLocalFiles checker = null;

    private ClientThread.BackgroundTasks bg = null;
    //private ClientThread.Login login = null;

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
            bg = new ClientThread.BackgroundTasks(localFolder, username, ip, port, this);

            //updater = new ClientThread.UpdateFilesFromServerClass(this, ip, port, 60, localFolder, username);
            //updater.start();

            //checker = new ClientThread.CheckForNewLocalFiles(this, ip, port, 5, localFolder, username);
            //checker.start();

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
            System.out.println("printText Exception : " + e.getMessage());
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

