package server;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import server.chat.ChatServer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * gui controller
 */
public class Controller {

    private int port, nThreads;
    private String path;

    private ServerThread server;
    private ServerThread.FileListUpdater updater;
    private HddController hddController = null;
    private ChatServer chatServer = null;

    //hdd folder names
    private String hdd1 = "hdd1";
    private String hdd2 = "hdd2";
    private String hdd3 = "hdd3";
    private String hdd4 = "hdd4";
    private String hdd5 = "hdd5";
    private ArrayList<String> hdd = new ArrayList<>();

    public Controller() {}

    @FXML private void initialize(){
        columnAllNames.setCellValueFactory(new PropertyValueFactory<>("filename"));
        columnAllSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        columnAllOwner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        columnAllOthers.setCellValueFactory(new PropertyValueFactory<>("others"));

        updateOperations(Arrays.asList(0, 0, 0, 0, 0));

        center.setDisable(true);
        bottom.setDisable(true);
        left.setDisable(true);
        buttonStop.setDisable(true);

        //send button is disabled when no test in msg field is entered
        textMsg.textProperty().addListener((observable, oldValue, newValue) ->{
            if(textMsg.getText().length() < 1){
                buttonSend.setDisable(true);
            }else{
                buttonSend.setDisable(false);
            }
        });

    }


    @FXML private HBox center;
    @FXML private HBox bottom;
    @FXML private VBox left;
    @FXML private ListView listUsers;
    @FXML private TableView<FileEntry> tableAll;
    @FXML private Label labelAllFiles;
    @FXML private Label labelAllOperations;
    @FXML private VBox boxAll;
    @FXML private TableColumn<FileEntry, String> columnAllOwner;
    @FXML private TableColumn<FileEntry, String> columnAllSize;
    @FXML private TableColumn<FileEntry, String> columnAllNames;
    @FXML private TableColumn<FileEntry, String> columnAllOthers;
    @FXML private Label labelHdd1Files;
    @FXML private Label labelHdd1Operations;
    @FXML private VBox boxHdd1;
    @FXML private Label labelHdd2Files;
    @FXML private Label labelHdd2Operations;
    @FXML private VBox boxHdd2;
    @FXML private Label labelHdd3Operations;
    @FXML private Label labelHdd3Files;
    @FXML private VBox boxHdd3;
    @FXML private Label labelHdd4Operations;
    @FXML private Label labelHdd4Files;
    @FXML private VBox boxHdd4;
    @FXML private Label labelHdd5Operations;
    @FXML private Label labelHdd5Files;
    @FXML private VBox boxHdd5;
    @FXML private Button buttonStart;
    @FXML private Button buttonStop;
    @FXML private Button buttonSend;
    @FXML private TextField inputPort;
    @FXML private TextField inputPath;
    @FXML private TextField textMsg;
    @FXML private TextArea textChat;

/*
//didn't work as expected
    @FXML private List<Label> hddOperations = Arrays.asList(labelHdd1Operations,labelHdd2Operations,
            labelHdd3Operations,labelHdd4Operations,labelHdd5Operations );
*/

    /**
     * send message to chat server
     */
    @FXML private void sendMsg(){
        if(chatServer != null)chatServer.receive("SERVER> "+textMsg.getText());
        textMsg.clear();
    }

    /**
     * display new mesage in chat text area
     * @param m new message
     */
    @FXML
    public void displayMsg(String m){
        textChat.appendText(m+"\n");
    }

    /**
     * update list of online users
     * @param users new list of online users
     */
    @FXML void updateUsersOnline(ObservableList<String> users){
        Platform.runLater(() -> {
            listUsers.getItems().clear();
            listUsers.getItems().setAll(users);
        });
    }

    /**
     * update list of files
     * @param f new list of files
     */
    @FXML void updateFiles(ArrayList<FileEntry> f){
        //update all files tab
        //System.out.println(" > update all files tab");
        ObservableList<FileEntry> listForGuiAll = FXCollections.observableArrayList();
        List<Integer> numberOfFiles = Arrays.asList(0, 0, 0, 0, 0);
        for(FileEntry fe : f){
            listForGuiAll.add(fe);
            numberOfFiles.set(fe.getHddNo()-1, 1+numberOfFiles.get(fe.getHddNo()-1));
        }
        //remove from new list or update entries that are already in gui
        for(Iterator<FileEntry> j = tableAll.getItems().iterator(); j.hasNext();){
            FileEntry fold = j.next();
            boolean fileFound = false;
            for(Iterator<FileEntry> i = listForGuiAll.iterator(); i.hasNext();){
                FileEntry fnew = i.next();
                if(fnew.getFilename().equals(fold.getFilename()) && fnew.getOwner().equals(fold.getOwner()) ){
                    //same file entry
                    fold.setOthers(fnew.getOthers());
                    if(fold.getSize() != fnew.getSize()){
                        fold.setSize(fnew.getSize());
                    }
                    fileFound = true;
                    //remove from new list entry that is already in gui
                    i.remove();
                    break;
                }
            }
            if(!fileFound){
                //remove from gui entry that is not present in new list
                j.remove();
            }
        }
        //add new entries to gui
        tableAll.getItems().addAll(listForGuiAll);

        //don't change, throws exception otherwise
        Platform.runLater(() -> labelAllFiles.setText(tableAll.getItems().size()+" files"));
        Platform.runLater(() -> labelHdd1Files.setText(numberOfFiles.get(0)+" files"));
        Platform.runLater(() -> labelHdd2Files.setText(numberOfFiles.get(1)+" files"));
        Platform.runLater(() -> labelHdd3Files.setText(numberOfFiles.get(2)+" files"));
        Platform.runLater(() -> labelHdd4Files.setText(numberOfFiles.get(3)+" files"));
        Platform.runLater(() -> labelHdd5Files.setText(numberOfFiles.get(4)+" files"));

    }

    /**
     * update list of files without checking for duplicates or updating existing entries
     * @param f new list of files
     */
    @FXML void updateFilesForce(ArrayList<FileEntry> f){
        tableAll.getItems().clear();
        tableAll.getItems().addAll(f);
    }

    /**
     * update number of operations on hdds
     * @param hddNumberOfOperations new list of numbers of operations
     */
    @FXML void updateOperations(List<Integer> hddNumberOfOperations){

        /*Platform.runLater(() -> {
            for (int i = 0; i < hddOperations.size() && i < hddNumberOfOperations.size(); i++) {
                String text = "" + hddNumberOfOperations.get(i);
                hddOperations.get(i).setText(text);
            }
        });*/
        // ^ looks good, but didn't work
        int i = 0;
        int sumOfOperations = 0;
        String orang = "-fx-background-color: #8d6b1c;";
        String green = "-fx-background-color: #0c5917;";
        String gray = "-fx-background-color: #444;";
        int whenOrang = 5;

        String text1 = "" + hddNumberOfOperations.get(i);
        if(hddNumberOfOperations.get(i) > whenOrang)
            boxHdd1.setStyle(orang);//orange
        else if(hddNumberOfOperations.get(i) > 0)
            boxHdd1.setStyle(green);//green
        else
            boxHdd1.setStyle(gray);//gray
        Platform.runLater(() -> labelHdd1Operations.setText("operations: " + text1));
        i++;
        String text2 = "" + hddNumberOfOperations.get(i);
        if(hddNumberOfOperations.get(i) > whenOrang)
            boxHdd2.setStyle(orang);//orange
        else if(hddNumberOfOperations.get(i) > 0)
            boxHdd2.setStyle(green);//green
        else
            boxHdd2.setStyle(gray);//gray
        Platform.runLater(() -> labelHdd2Operations.setText("operations: " + text2));
        i++;
        String text3 = "" + hddNumberOfOperations.get(i);
        if(hddNumberOfOperations.get(i) > whenOrang)
            boxHdd3.setStyle(orang);//orange
        else if(hddNumberOfOperations.get(i) > 0)
            boxHdd3.setStyle(green);//green
        else
            boxHdd3.setStyle(gray);//gray
        Platform.runLater(() -> labelHdd3Operations.setText("operations: " + text3));
        i++;
        String text4 = "" + hddNumberOfOperations.get(i);
        if(hddNumberOfOperations.get(i) > whenOrang)
            boxHdd4.setStyle(orang);//orange
        else if(hddNumberOfOperations.get(i) > 0)
            boxHdd4.setStyle(green);//green
        else
            boxHdd4.setStyle(gray);//gray
        Platform.runLater(() -> labelHdd4Operations.setText("operations: " + text4));
        i++;
        String text5 = "" + hddNumberOfOperations.get(i);
        if(hddNumberOfOperations.get(i) > whenOrang)
            boxHdd5.setStyle(orang);//orange
        else if(hddNumberOfOperations.get(i) > 0)
            boxHdd5.setStyle(green);//green
        else
            boxHdd5.setStyle(gray);//gray
        Platform.runLater(() -> labelHdd5Operations.setText("operations: " + text5));

        for(Integer n : hddNumberOfOperations)
            sumOfOperations += n;
        String textAll = "" + sumOfOperations;
        if(hddNumberOfOperations.get(i) > whenOrang)
            boxHdd1.setStyle(orang);//orange
        else if(hddNumberOfOperations.get(i) > 0)
            boxAll.setStyle(green);//green
        else
            boxAll.setStyle(gray);
        Platform.runLater(() -> labelAllOperations.setText("operations: " + textAll));

    }

    /**
     * creates new dialog window
     * @param title title of dialog window
     * @param text test to be displayed in this dialog window
     */
    @FXML void dialog(String title, String text){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    /**
     * stars server when "Start server" button is clicked
     * @param event button event
     */
    @FXML void Start(ActionEvent event) {
        event.consume();
        buttonStart.setDisable(true);
        inputPath.setDisable(true);
        inputPort.setDisable(true);
        boolean validFlag;
        try {
            path = inputPath.getText();
            port = Integer.parseInt(inputPort.getText());
            nThreads = 100;//default value
            validFlag = true;
        } catch (Exception e) {
            validFlag = false;
        }
        if(validFlag){
            hdd.add(path + File.separator + File.separator + hdd1);
            hdd.add(path + File.separator + File.separator + hdd2);
            hdd.add(path + File.separator + File.separator + hdd3);
            hdd.add(path + File.separator + File.separator + hdd4);
            hdd.add(path + File.separator + File.separator + hdd5);
            buttonStop.setDisable(false);
            hddController = new HddController(this);
            server = new ServerThread(port, nThreads, path, hdd, hddController, this);
            updater = new ServerThread.FileListUpdater(hdd, this, server, 10);
            chatServer = new ChatServer(port, this);
            chatServer.start();
            updater.start();
            //wait till updater finishes init
            while(!updater.initialized){
                try{
                    Thread.sleep(100);
                }catch(InterruptedException e){}
            }
            server.start();
            center.setDisable(false);
            bottom.setDisable(false);
            left.setDisable(false);
        }else{
            buttonStart.setDisable(false);
            inputPath.setDisable(false);
            inputPort.setDisable(false);
            dialog("SuperServer - error", "Something went wrong. Check if entered values are correct.");
        }
    }

    /**
     * stops server when "Stop server" button is clicked
     * @param event button event
     */
    @FXML void Stop(ActionEvent event) {
        event.consume();
        server.stop();
        updater.stop();
        chatServer.stop();

        buttonStop.setDisable(true);
        inputPath.setDisable(false);
        inputPort.setDisable(false);
        buttonStart.setDisable(false);

        center.setDisable(true);
        bottom.setDisable(true);
        left.setDisable(true);
    }

    /**
     * stops server when "X" button is clicked
     */
    void shutdown(){
        chatServer.receive("!!exit");
        if(server != null)server.stop();
        if(updater != null)updater.stop();
        if(chatServer != null)chatServer.stop();
    }

}
