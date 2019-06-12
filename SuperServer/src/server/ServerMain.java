package server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class ServerMain extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("server_window.fxml"));

        //Parent root = FXMLLoader.load(getClass().getResource("server_window.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        primaryStage.setTitle("SuperServer");
        primaryStage.setScene(new Scene(root, 1000, 600));
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            //if dialogbox true
            Alert alert = new Alert(Alert.AlertType.WARNING, "Do wou want to close server?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                controller.shutdown();
                Platform.exit();
            }
        });
        primaryStage.show();
    }
}
