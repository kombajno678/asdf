package server;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;

public class Controller {

    private int port, nThreads;
    private String path;

    private ServerThread server;
    private ServerThread.FileListUpdater updater;

    //hdd folder names
    String hdd1 = "hdd1";
    String hdd2 = "hdd2";
    String hdd3 = "hdd3";
    String hdd4 = "hdd4";
    String hdd5 = "hdd5";

    ArrayList<String> hdd = new ArrayList<>();

    public Controller() {}

    @FXML private void initialize(){
        columnHdd1Names.setCellValueFactory(new PropertyValueFactory<>("filename"));
        columnHdd1Size.setCellValueFactory(new PropertyValueFactory<>("size"));
        columnHdd1Owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        columnHdd1Others.setCellValueFactory(new PropertyValueFactory<>("others"));

        columnHdd2Names.setCellValueFactory(new PropertyValueFactory<>("filename"));
        columnHdd2Size.setCellValueFactory(new PropertyValueFactory<>("size"));
        columnHdd2Owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        columnHdd2Others.setCellValueFactory(new PropertyValueFactory<>("others"));

        columnHdd3Names.setCellValueFactory(new PropertyValueFactory<>("filename"));
        columnHdd3Size.setCellValueFactory(new PropertyValueFactory<>("size"));
        columnHdd3Owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        columnHdd3Others.setCellValueFactory(new PropertyValueFactory<>("others"));

        columnHdd4Names.setCellValueFactory(new PropertyValueFactory<>("filename"));
        columnHdd4Size.setCellValueFactory(new PropertyValueFactory<>("size"));
        columnHdd4Owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        columnHdd4Others.setCellValueFactory(new PropertyValueFactory<>("others"));

        columnHdd5Names.setCellValueFactory(new PropertyValueFactory<>("filename"));
        columnHdd5Size.setCellValueFactory(new PropertyValueFactory<>("size"));
        columnHdd5Owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        columnHdd5Others.setCellValueFactory(new PropertyValueFactory<>("others"));
    }

    @FXML private ListView listUsers;

    @FXML private TableView<FileEntry> tableHdd1;
    @FXML private Label labelHdd1Files;
    @FXML private Label labelHdd1Operations;
    @FXML private TableColumn<FileEntry, String> columnHdd1Owner;
    @FXML private TableColumn<FileEntry, String> columnHdd1Size;
    @FXML private TableColumn<FileEntry, String> columnHdd1Names;
    @FXML private TableColumn<FileEntry, String> columnHdd1Others;

    @FXML private TableView<FileEntry> tableHdd2;
    @FXML private TableColumn<FileEntry, String> columnHdd2Names;
    @FXML private TableColumn<FileEntry, String> columnHdd2Size;
    @FXML private TableColumn<FileEntry, String> columnHdd2Others;
    @FXML private TableColumn<FileEntry, String> columnHdd2Owner;
    @FXML private Label labelHdd2Files;
    @FXML private Label labelHdd2Operations;

    @FXML private TableView<FileEntry> tableHdd3;
    @FXML private TableColumn<FileEntry, String> columnHdd3Others;
    @FXML private TableColumn<FileEntry, String> columnHdd3Size;
    @FXML private TableColumn<FileEntry, String> columnHdd3Owner;
    @FXML private TableColumn<FileEntry, String> columnHdd3Names;
    @FXML private Label labelHdd3Operations;
    @FXML private Label labelHdd3Files;

    @FXML private TableView<FileEntry> tableHdd4;
    @FXML private TableColumn<FileEntry, String> columnHdd4Owner;
    @FXML private TableColumn<FileEntry, String> columnHdd4Size;
    @FXML private TableColumn<FileEntry, String> columnHdd4Names;
    @FXML private TableColumn<FileEntry, String> columnHdd4Others;
    @FXML private Label labelHdd4Operations;
    @FXML private Label labelHdd4Files;

    @FXML private TableView<FileEntry> tableHdd5;
    @FXML private TableColumn<FileEntry, String> columnHdd5Owner;
    @FXML private TableColumn<FileEntry, String> columnHdd5Size;
    @FXML private TableColumn<FileEntry, String> columnHdd5Others;
    @FXML private TableColumn<FileEntry, String> columnHdd5Names;
    @FXML private Label labelHdd5Operations;
    @FXML private Label labelHdd5Files;

    @FXML private Button buttonStart;
    @FXML private Button buttonStop;

    @FXML private TextField inputPort;
    @FXML private TextField inputPath;


    public void updateUsersOnline(ObservableList<String> users){
        Platform.runLater(() -> {
            listUsers.getItems().clear();
            listUsers.getItems().setAll(users);
        });
    }
    public void updateFiles(ObservableList<FileEntry> f, int hdd ){
        TableView<FileEntry> t;
        Label files;
        switch(hdd){
            case 1:
                t = tableHdd1;
                files = labelHdd1Files;
                break;
            case 2:
                t = tableHdd2;
                files = labelHdd2Files;
                break;
            case 3:
                t = tableHdd3;
                files = labelHdd3Files;
                break;
            case 4:
                t = tableHdd4;
                files = labelHdd4Files;
                break;
            case 5:
                t = tableHdd5;
                files = labelHdd5Files;
                break;
            default:
                return;
        }
        //remove what is not in new list of files
        for(FileEntry fold : t.getItems())
        {
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
                t.getItems().removeAll(fold);
            }else{
                f.removeAll(del);
            }

        }
        //add what is not present in current list of files
        if(f.size() > 0)
            for(FileEntry fnew : f){
                t.getItems().addAll(fnew);
            }

        //don't change, throws exception otherwise
        Platform.runLater(() -> files.setText(t.getItems().size()+" files"));
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
            ArrayList<String> hdd = new ArrayList<>();
            hdd.add(path + "\\" + hdd1);
            hdd.add(path + "\\" + hdd2);
            hdd.add(path + "\\" + hdd3);
            hdd.add(path + "\\" + hdd4);
            hdd.add(path + "\\" + hdd5);

            server = new ServerThread(port, nThreads, path, hdd);

            updater = new ServerThread.FileListUpdater(hdd, this, server, 10);
            updater.start();

            buttonStop.setDisable(false);

            inputPath.setDisable(true);
            inputPort.setDisable(true);
            buttonStart.setDisable(true);
        }
    }

    @FXML void Stop(ActionEvent event) {
        event.consume();
        server.stop();
        updater.stop();
        buttonStop.setDisable(true);

        inputPath.setDisable(false);
        inputPort.setDisable(false);
        buttonStart.setDisable(false);
    }

}
