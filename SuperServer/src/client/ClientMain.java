package client;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class ClientMain extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("client_window_2.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        primaryStage.setTitle("SuperClient");
        Scene scene = new Scene(root, 800, 600);
        //scene.getStylesheets().add("client.css");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            //if dialogbox true
            Alert alert = new Alert(Alert.AlertType.WARNING, "Do wou want to close client?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                controller.shutdown();
                Platform.exit();
            }
        });
        primaryStage.show();

    }
    public static void main(String[] args) {
        launch(args);
    }
}
