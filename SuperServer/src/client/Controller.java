package client;

import client.chat.ChatListener;
import client.chat.ChatSpeaker;
import client.game.IntValue;
import client.game.LongValue;
import client.game.Sprite;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import server.FileEntry;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Gui controller
 */
public class Controller {
    private String username;
    private String localFolder;
    private String ip;
    private int port;
    private static int listenerPort;
    private static int speakerPort;
    private ChatListener listener;
    private ChatSpeaker speaker;
    private BackgroundTasks bg = null;

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
        columnSize.setCellValueFactory(new PropertyValueFactory<>("sizeString"));
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

    /**
     * contains code for game
     */
    private void bricksGame(){
        Stage primaryStage = new Stage();
        primaryStage.setTitle( "BreakBricks" );
        Group root = new Group();
        Scene theScene = new Scene( root );
        primaryStage.setScene( theScene );
        theScene.setFill(new Color(0.05, 0.05, 0.15, 1));
        Canvas canvas = new Canvas( 512, 512 );
        root.getChildren().add( canvas );
        primaryStage.setMinHeight(552);
        primaryStage.setMinWidth(528);
        primaryStage.setMaxHeight(628);
        primaryStage.setMaxWidth(528);
        ArrayList<String> input = new ArrayList<>();
        theScene.setOnKeyPressed(e -> {
            String code = e.getCode().toString();
            if ( !input.contains(code) )
                input.add( code );
        });
        theScene.setOnKeyReleased(e -> {
            String code = e.getCode().toString();
            input.remove( code );
        });
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Font theFont = Font.font( "Arial", FontWeight.BOLD, 24 );
        gc.setFont( theFont );
        gc.setFill( Color.WHITE );
        //gc.setStroke( Color.BLACK );
        //gc.setLineWidth(1);

        Sprite paddle = new Sprite();
        paddle.setImage("client\\game\\paddle.png");
        paddle.setPosition(256-(paddle.getWidth()/2), 512-paddle.getHeight());
        paddle.speed = 400;

        final ArrayList<Sprite> brickList = new ArrayList<>();

        for (int i = 0; i < 16; i++)
        {
            for(int j = 4; j < 10; j++){
                Sprite brick = new Sprite();
                //int randomNum = ThreadLocalRandom.current().nextInt(0, 8 + 1);
                brick.setImage("client\\game\\brick"+(j-4)+".png");
                brick.score = (j-10)*-100;
                double px = i*32;
                double py = j*20;
                brick.setPosition(px,py);
                brickList.add( brick );
            }
        }

        Sprite ball = new Sprite();
        ball.setImage("client\\game\\ball.png");
        ball.setPosition(256, 400);
        ball.setVelocity(0, -200);
        ball.speed = 200;

        LongValue lastNanoTime = new LongValue( System.nanoTime() );
        IntValue score = new IntValue(0);

        final int paddleSpeed = 400;

        new AnimationTimer()
        {
            private boolean playing = true;
            public void handle(long currentNanoTime)
            {
                // calculate time since last update.
                double elapsedTime = (currentNanoTime - lastNanoTime.value) / 1000000000.0;
                lastNanoTime.value = currentNanoTime;

                if(!playing){
                    //game over
                    // render
                    gc.clearRect(0, 0, 512,512);
                    paddle.render( gc );
                    ball.render(gc);
                    for (Sprite brick : brickList )
                        brick.render( gc );

                    String pointsText = "SCORE: " + (score.value);
                    gc.fillText( pointsText, 36, 36 );
                    String gameOver = "GAME OVER";
                    gc.fillText(gameOver, 180, 256);
                    String info = "press up arrow to play again";
                    gc.fillText(info, 100, 276);

                    // button input
                    paddle.setVelocity(0,0);
                    if (input.contains("UP")){
                        //prepare for next game
                        score.value = 0;
                        brickList.clear();
                        for (int i = 0; i < 16; i++)
                        {
                            for(int j = 4; j < 10; j++){
                                Sprite brick = new Sprite();
                                brick.setImage("client\\game\\brick"+(j-4)+".png");
                                brick.score = (j-10)*-100;
                                double px = i*32;
                                double py = j*20;
                                brick.setPosition(px,py);
                                brickList.add( brick );
                            }
                        }
                        paddle.setPosition(256-(paddle.getWidth()/2), 512-paddle.getHeight());
                        ball.setPosition(256, 386);
                        ball.setVelocity(0, -200);
                        ball.speed = 200;
                        playing = true;
                    }


                }else{
                    // button input
                    paddle.setVelocity(0,0);
                    if (input.contains("LEFT"))
                        paddle.addVelocity(-paddleSpeed,0);
                    if (input.contains("RIGHT"))
                        paddle.addVelocity(paddleSpeed,0);


                    //paddle must stay within bounds
                    if(paddle.getBoundary().getMinX() <= 0 ){
                        //paddle.setVelocity(0, 0);
                        paddle.setPositionX(0);
                    }else if(paddle.getBoundary().getMinX() >= 512 - paddle.getWidth()){
                        paddle.setPositionX(512 - paddle.getWidth());
                    }

                    // ball - bricks collision detection
                    Iterator<Sprite> brickIter = brickList.iterator();
                    while ( brickIter.hasNext() ) {
                        Sprite brick = brickIter.next();
                        if ( ball.intersects(brick) ) {

                            brickIter.remove();
                            score.value += brick.score;
                            ball.bounce(brick);
                            ball.speed += 10;
                            break;
                        }
                    }

                    //bounce ball of paddle

                    if(ball.intersects(paddle)){//0,1s
                        ball.bounceOfPaddle(paddle);

                    }

                    //bounce ball of walls
                    if(ball.getPositionX() <= 0 || ball.getPositionX()+ball.getWidth() >= 512){
                        ball.setVelocityX(ball.getVelocityX() * -1);
                    }
                    if(ball.getPositionY()+ball.getHeight() >= 512){
                        ball.setVelocity(0, 0);
                        playing = false;

                    }
                    if(ball.getPositionY() <= 0){
                        ball.setVelocityY(ball.getVelocityY() * -1);
                    }

                    //make sure that ball is in bounds
                    if(ball.getPositionX() <= 0){
                        ball.setPositionX(0);
                    }
                    if(ball.getPositionX() + ball.getWidth() >= 512){
                        ball.setPositionX(512 - ball.getWidth());
                    }
                    if(ball.getPositionY() <= 0){
                        ball.setPositionY(0);
                    }
                    if(ball.getPositionY() + ball.getHeight() >= 512){
                        ball.setPositionY(512 - ball.getHeight());
                    }


                    // render
                    gc.clearRect(0, 0, 512,512);
                    paddle.update(elapsedTime);
                    ball.normalizeSpeed();
                    ball.update(elapsedTime);
                    paddle.render( gc );
                    ball.render(gc);
                    for (Sprite brick : brickList )
                        brick.render( gc );

                    String pointsText = "SCORE: " + (score.value);
                    gc.fillText( pointsText, 36, 36 );
                }


            }
        }.start();

        Platform.runLater(()-> primaryStage.show());
    }

    /**
     * launches game
     * @param event button event
     */
    @FXML void launchGame(Event event){
        event.consume();
        bricksGame();
    }

    /**
     * sets text on small label in lower left corner of window
     * @param text new string to set
     */
    @FXML void setTextLeft(String text){
        Platform.runLater(() -> textBotLeft.setText(text));
    }

    /**
     * appends text on small label in lower left corner of window
     * @param text string to append
     */
    @FXML void addTextLeft(String text){
        Platform.runLater(()->textBotLeft.setText(textBotLeft.getText()+text));
    }

    /**
     * opens a dialog window
     * @param title title for window
     * @param text text to be displayed in window
     */
    @FXML void dialog(String title, String text){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    /**
     * checks what file is selected in table and invokes method in BackgroundTasks to delete this file
     * @param event button event
     */
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

    /**
     * adds username to mesage from input field
     * invokes sendMsg on chat speaker to send this message
     * clears input field
     * @param event button event
     */
    @FXML
    void sendMsg(ActionEvent event) {
        event.consume();
        speaker.sendMsg(username+"> "+textMsg.getText());
        textMsg.setText("");
    }

    /**
     * displays chat message
     * @param m message to be displayed
     */
    @FXML
    public synchronized void displayMsg(String m){
        textChat.appendText(m+"\n");
    }

    /**
     * invoked when "Connect" button is pressed
     * validates values from input fields
     * starts new threads: ClientThread.BackgroundTasks, ChatListener, ChatSpeaker
     */
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
            bg = new BackgroundTasks(localFolder, username, ip, port, this, 5);
            bg.start();

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

    /**
     * invoked when "Disonnect" button is pressed
     * tries to stop all threads
     */
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

    /**
     * invoked when "X" button is pressed
     * tries to stop all threads
     */
    public void shutdown(){
        if(bg != null)bg.stop();
        if(listener != null)listener.stop();
        if(speaker != null)speaker.stop();
    }

    /**
     * invoked when "Share" button is pressed
     * checks what file is selected on list
     * sends this file to method in BackgroundTasks
     */
    public void share(){
        FileEntry file = tableFiles.getItems().get(tableFiles.getFocusModel().getFocusedCell().getRow());
        //file : selected file on list
        Platform.runLater(() -> printText("sharing : " + file));
        if(file.getOwner().equals(username)){
            bg.share(file);
        }
    }

    /**
     * invoked when "Unhare" button is pressed
     * checks what file is selected on list
     * sends this file to method in BackgroundTasks
     */
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
    /**
     * invoked when "Clear console" button is pressed
     * clears all text from console
     */
    public void clear(){
        textConsole.clear();
    }

    /**
     * prints text on console
     * @param a text to print
     */
    public synchronized void printText(String a){
        try {
            textConsole.appendText(a + "\n");
        }catch(Exception e){
            System.out.println("printText Exception : " + e.getMessage());
        }
    }

    /**
     * clears list of files
     */
    public void clearFiles(){
        tableFiles.getItems().clear();
    }

    /**
     * updates list of files
     * @param f new list of files
     */
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
                        fold = fnew;
                        /*
                        fold.setOthers(fnew.getOthers());
                        if(fold.getSize() != fnew.getSize()){
                            fold.setSize(fnew.getSize());
                        }
                        */
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
