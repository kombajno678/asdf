package server;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Controller {

    private int port, nThreads;
    private String path;

    private ServerThread server;
    private ServerThread.FileListUpdater updater;
    private HddController hddController = null;
    private ChatServer chatServer = null;
    //private ServerThread.CsvReadWrite csv;

    //hdd folder names
    String hdd1 = "hdd1";
    String hdd2 = "hdd2";
    String hdd3 = "hdd3";
    String hdd4 = "hdd4";
    String hdd5 = "hdd5";

    ArrayList<String> hdd = new ArrayList<>();

    //List<Integer> hddNumberOfOperations = Arrays.asList(0, 0, 0, 0, 0);

    public Controller() {}

    @FXML private void initialize(){
        columnAllNames.setCellValueFactory(new PropertyValueFactory<>("filename"));
        columnAllSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        columnAllOwner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        columnAllOthers.setCellValueFactory(new PropertyValueFactory<>("others"));
        updateOperations(Arrays.asList(0, 0, 0, 0, 0));
    }

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

    @FXML private TextField inputPort;
    @FXML private TextField inputPath;

    @FXML private TextField textMsg;
    @FXML private TextArea textChat;


    @FXML private List<Label> hddOperations = Arrays.asList(labelHdd1Operations,labelHdd2Operations,
            labelHdd3Operations,labelHdd4Operations,labelHdd5Operations );

    @FXML private void sendMsg(){
        chatServer.receive("SERVER>"+textMsg.getText());
        textMsg.setText("");
    }
    @FXML
    void displayMsg(String m){
        textChat.setText(textChat.getText()+"\n"+m);
    }

    @FXML public void updateUsersOnline(ObservableList<String> users){
        Platform.runLater(() -> {
            listUsers.getItems().clear();
            listUsers.getItems().setAll(users);
        });
    }
    @FXML public void updateFiles(ArrayList<FileEntry> f){
        //update all files tab
        //-------------------------------------
        ObservableList<FileEntry> listForGuiAll = FXCollections.observableArrayList();
        List<Integer> numberOfFiles = Arrays.asList(0, 0, 0, 0, 0);
        for(FileEntry fe : f){
            listForGuiAll.add(fe);
            numberOfFiles.set(fe.getHddNo()-1, 1+numberOfFiles.get(fe.getHddNo()-1));
        }
        //remove from new list or update entries that are already in gui
        //for(FileEntry fold : tableAll.getItems())
        for(Iterator<FileEntry> j = tableAll.getItems().iterator(); j.hasNext();)
        {
            FileEntry fold = j.next();
            boolean fileFound = false;
            for(Iterator<FileEntry> i = listForGuiAll.iterator(); i.hasNext();){
                FileEntry fnew = i.next();
                if(fnew.getFilename().equals(fold.getFilename()) && fnew.getOwner().equals(fold.getOwner())){
                    //same file entry
                    if(!fold.getOthers().equals(fnew.getOthers())){
                        fold.setOthers(fnew.getOthers());
                    }
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
                //tableAll.getItems().removeAll(fold);
                j.remove();
            }
        }
        //add new entries to gui
        if(listForGuiAll.size() > 0)
            for(FileEntry fnew : listForGuiAll)
                tableAll.getItems().addAll(fnew);

        //don't change, throws exception otherwise
        Platform.runLater(() -> labelAllFiles.setText(tableAll.getItems().size()+" files"));
        //-------------------------------------
        Platform.runLater(() -> labelHdd1Files.setText(numberOfFiles.get(0)+" files"));
        Platform.runLater(() -> labelHdd2Files.setText(numberOfFiles.get(1)+" files"));
        Platform.runLater(() -> labelHdd3Files.setText(numberOfFiles.get(2)+" files"));
        Platform.runLater(() -> labelHdd4Files.setText(numberOfFiles.get(3)+" files"));
        Platform.runLater(() -> labelHdd5Files.setText(numberOfFiles.get(4)+" files"));

    }

    @FXML public void updateOperations(List<Integer> hddNumberOfOperations){

        /*Platform.runLater(() -> {
            for (int i = 0; i < hddOperations.size() && i < hddNumberOfOperations.size(); i++) {
                String text = "" + hddNumberOfOperations.get(i);
                hddOperations.get(i).setText(text);
            }
        });*/
        int i = 0;
        int sumOfOperations = 0;
        String green = "-fx-background-color: #af7;";
        String gray = "-fx-background-color: #ddd;";

        String text1 = "" + hddNumberOfOperations.get(i);
        if(hddNumberOfOperations.get(i) > 0)
            boxHdd1.setStyle(green);//green
        else
            boxHdd1.setStyle(gray);//gray
        Platform.runLater(() -> labelHdd1Operations.setText(text1));
        i++;
        String text2 = "" + hddNumberOfOperations.get(i);
        if(hddNumberOfOperations.get(i) > 0)
            boxHdd2.setStyle(green);//green
        else
            boxHdd2.setStyle(gray);//gray
        Platform.runLater(() -> labelHdd2Operations.setText(text2));
        i++;
        String text3 = "" + hddNumberOfOperations.get(i);
        if(hddNumberOfOperations.get(i) > 0)
            boxHdd3.setStyle(green);//green
        else
            boxHdd3.setStyle(gray);//gray
        Platform.runLater(() -> labelHdd3Operations.setText(text3));
        i++;
        String text4 = "" + hddNumberOfOperations.get(i);
        if(hddNumberOfOperations.get(i) > 0)
            boxHdd4.setStyle(green);//green
        else
            boxHdd4.setStyle(gray);//gray
        Platform.runLater(() -> labelHdd4Operations.setText(text4));
        i++;
        String text5 = "" + hddNumberOfOperations.get(i);
        if(hddNumberOfOperations.get(i) > 0)
            boxHdd5.setStyle(green);//green
        else
            boxHdd5.setStyle(gray);//gray
        Platform.runLater(() -> labelHdd5Operations.setText(text5));

        for(Integer n : hddNumberOfOperations)
            sumOfOperations += n;
        String textAll = "" + sumOfOperations;
        if(hddNumberOfOperations.get(i) > 0)
            boxAll.setStyle(green);//green
        else
            boxAll.setStyle(gray);
        Platform.runLater(() -> labelAllOperations.setText(textAll));

    }

    @FXML void Start(ActionEvent event) {
        event.consume();
        boolean validFlag = false;
        try {
            path = inputPath.getText();
            port = Integer.parseInt(inputPort.getText());
            nThreads = 100;//default value
            validFlag = true;
        } catch (Exception e) {
            validFlag = false;
        }
        if(validFlag){
            //ArrayList<String> hdd = new ArrayList<>();
            hdd.add(path + File.separator + File.separator + hdd1);
            hdd.add(path + File.separator + File.separator + hdd2);
            hdd.add(path + File.separator + File.separator + hdd3);
            hdd.add(path + File.separator + File.separator + hdd4);
            hdd.add(path + File.separator + File.separator + hdd5);

            buttonStop.setDisable(false);

            inputPath.setDisable(true);
            inputPort.setDisable(true);
            buttonStart.setDisable(true);

            hddController = new HddController(this);
            server = new ServerThread(port, nThreads, path, hdd, hddController);
            updater = new ServerThread.FileListUpdater(hdd, this, server, 10);
            chatServer = new ChatServer(this);
            chatServer.start();
            updater.start();
            //wait till updater finishes init
            while(!updater.initialized){
                try{
                    Thread.sleep(100);
                }catch(InterruptedException e){}
            }
            server.start();


        }
    }

    @FXML void Stop(ActionEvent event) {
        event.consume();
        server.stop();
        updater.stop();
        chatServer.stop();
        //csv.stop();
        buttonStop.setDisable(true);

        inputPath.setDisable(false);
        inputPort.setDisable(false);
        buttonStart.setDisable(false);
    }

    public void shutdown(){
        if(server != null)server.stop();
        if(updater != null)updater.stop();
        if(chatServer != null)chatServer.stop();
    }

}
