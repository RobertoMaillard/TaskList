package com.robertomaillard.tasklist;

import com.robertomaillard.tasklist.datamodel.TaskData;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Created by Roberto Maillard on 19/9/2019.
 */

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        try {
            // SPECIFY DATA SOURCE. DB or XML.
            TaskData.getInstance().loadTaskItems(TaskData.DataSource.DB);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("window.fxml"));
        primaryStage.setTitle("TASK LIST");
        primaryStage.setScene(new Scene(root, 809, 500));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
    }
}
