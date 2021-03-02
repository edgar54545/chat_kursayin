package kursayin.team0.frames;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ErrorAlertBox {

    public static void display(String errorMessage) {
        Stage stage = new Stage();
        stage.setTitle("Something wrong!");
        stage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label();
        label.setText(errorMessage);
        label.setAlignment(Pos.TOP_CENTER);

        Button button = new Button("Got it");
        button.setOnAction(e -> stage.close());
        button.setDefaultButton(true);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setMinWidth(200);
        layout.setMinHeight(50);
        layout.getChildren().addAll(label, button);

        Scene scene = new Scene(layout);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.showAndWait();
    }
}