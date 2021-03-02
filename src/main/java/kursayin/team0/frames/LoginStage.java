package kursayin.team0.frames;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kursayin.team0.client.Client;
import kursayin.team0.db.Database;
import kursayin.team0.regexes.URLRegEx;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LoginStage extends Application {
    private Stage primaryStage;
    private VBox layout;
    private Scene scene;
    private Button loginButton;
    private Label urlLabel, usernameLabel;
    private TextField urlField, usernameField;
    private Insets insets;
    private HBox layoutForButton;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Database database;

    @Override
    public void start(Stage primaryStage) {
        database = new Database();
        try {
            database.createConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Login");

        primaryStage.setOnCloseRequest(e -> System.exit(0));

        urlLabel = new Label("Enter URL here");
        urlField = new TextField("tcp://localhost:9001");
        usernameLabel = new Label("Enter your username here");
        usernameField = new TextField();
        usernameField.lengthProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue.intValue() > oldValue.intValue()) {
                if (usernameField.getText().length() >= 30) {
                    usernameField.setText(usernameField.getText(0, 30));
                }
            }
        });

        loginButton = new Button("Log in");
        loginButton.setOnAction(e -> tryToLogin());
        loginButton.setDefaultButton(true);

        insets = new Insets(15);
        layoutForButton = new HBox();
        layoutForButton.setPadding(insets);
        layoutForButton.getChildren().add(loginButton);
        layoutForButton.setAlignment(Pos.BASELINE_RIGHT);

        layout = new VBox(7);
        layout.setPadding(insets);
        layout.getChildren().addAll(urlLabel, urlField, usernameLabel, usernameField, layoutForButton);

        scene = new Scene(layout, 300, 180);
        primaryStage.setScene(scene);

        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void tryToLogin() {
        String url = urlField.getText();
        String username = usernameField.getText();
        URLRegEx regEx = new URLRegEx();
        if (url.equals("") || username.equals("")) {
            ErrorAlertBox.display("URL or username\ncan not be empty!");
        } else if (!regEx.isValidURL(url)) {
            ErrorAlertBox.display("URL is not valid!");
        } else {
            try {
                if (database.isUsernameFree(username)) {
                    String host = regEx.getHost();
                    Integer port = regEx.getPort();
                    ChatStage chatStage = new ChatStage(username, database.getPreviousConversation(username));
                    chatStage.display(primaryStage);
                    Client client = new Client(host, port, username, chatStage, database);
                    chatStage.setClient(client);
                    executorService.submit(client);
                } else {
                    ErrorAlertBox.display("Name is used now!\nUse another name!");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


}
