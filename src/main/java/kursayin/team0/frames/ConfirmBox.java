package kursayin.team0.frames;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConfirmBox {

    public static boolean confirm;

    public static void display() {
        Stage stage = new Stage();
        stage.setTitle("Leaving chat...");
        stage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label();
        label.setText("Are you sure you want to\nleave the conversation");
        label.setAlignment(Pos.TOP_CENTER);

        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));
        mainLayout.setAlignment(Pos.TOP_CENTER);

        Button yesButton = new Button("Yes");
        yesButton.setOnAction(e -> {
            confirm = true;
            stage.close();
        });
        Button noButton = new Button("No");
        noButton.setOnAction(e -> {
            confirm = false;
            stage.close();
        });
        noButton.setDefaultButton(true);

        HBox buttonLayout = new HBox(25);
        buttonLayout.setAlignment(Pos.TOP_CENTER);
        buttonLayout.getChildren().addAll(yesButton, noButton);

        mainLayout.getChildren().addAll(label, buttonLayout);

        Scene scene = new Scene(mainLayout);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT) {
                yesButton.setDefaultButton(true);
                noButton.setDefaultButton(false);
            } else if (e.getCode() == KeyCode.RIGHT) {
                yesButton.setDefaultButton(false);
                noButton.setDefaultButton(true);
            }
        });

        stage.setScene(scene);
        stage.setResizable(false);
        stage.showAndWait();
    }
}