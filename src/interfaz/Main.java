package interfaz;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
            Parent root = loader.load();
            primaryStage.setTitle("Tarea 2 RIT");
            primaryStage.setScene(new Scene(root, 500, 500));
            primaryStage.setResizable(false);
            primaryStage.show();
            ControllerMain c = loader.getController();
            primaryStage.setOnCloseRequest(event -> {
                if(c.indexer != null){
                    try {
                        c.indexer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
