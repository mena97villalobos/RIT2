package interfaz;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
