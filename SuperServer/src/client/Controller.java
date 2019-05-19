package client;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import jdk.nashorn.internal.ir.annotations.Ignore;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Controller {

    private String username;// = "adamko";
    private String localFolder;// = "local\\"+username;
    private String ip;// = "127.0.0.1";
    private int port;// = 55555;
    private ClientThread.UpdateFilesFromServerClass updater = null;
    private ClientThread.CheckForNewLocalFiles checker = null;

    public Controller(){}

    @FXML
    private void initialize(){
        columnNames.setCellValueFactory(new PropertyValueFactory<>("filename"));
        columnShared.setCellValueFactory(new PropertyValueFactory<>("sharedTo"));
        columnStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }
    @FXML
    private Label textBotLeft;
    @FXML
    private Label textBotRight;
    @FXML
    private Button buttonDelete;
    @FXML
    private TableView<FileEntry> tableFiles;
    @FXML
    private TableColumn<FileEntry, String> columnNames;
    @FXML
    private TableColumn<FileEntry, String> columnShared;
    @FXML
    private TableColumn<FileEntry, String> columnStatus;
    @FXML
    public TextArea textConsole;
    @FXML
    private Button buttonShare;
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
            localFolder = inputPath.getText() + "\\" + username;
            ip = inputIP.getText();
            port = Integer.parseInt(inputPort.getText());
            validFlag = true;
        } catch (Exception e) {
            validFlag = false;
        }
        if(validFlag){
            updater = new ClientThread.UpdateFilesFromServerClass(this, ip, port, 60, localFolder, username);
            //updater.start();

            checker = new ClientThread.CheckForNewLocalFiles(this, ip, port, 5, localFolder, username);
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

            textConsole.setDisable(false);
            textConsole.setText("Connected to " + ip + "\n");
        }

    }
    public void Disconnect() {
        updater.stop();
        checker.stop();
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
    }

    public void Clear(){
        textConsole.clear();
    }
    public void printText(String a){
        try {
            Platform.runLater(() -> {
                textConsole.setText(textConsole.getText() + "\n" + a);
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
    public void updateFiles(ObservableList<FileEntry> f){
        //remove what is not in new list of files
        for(FileEntry fold : tableFiles.getItems()){
            boolean fileFound = false;
            FileEntry del = null;
            for(FileEntry fnew : f){
                if(fnew.getFilename().equals(fold.getFilename())){
                    fileFound = true;
                    del = fnew;
                    break;
                }
            }
            if(!fileFound){
                tableFiles.getItems().removeAll(fold);
            }else{
                f.removeAll(del);
            }

        }
        //add what is not present in current list of files
        if(f.size() > 0)
            for(FileEntry fnew : f){
                tableFiles.getItems().addAll(fnew);
            }
    }
}

