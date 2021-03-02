package kursayin.team0.frames;

//    tcp://localhost:9001

import aca.proto.ChatMsg;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import kursayin.team0.client.Client;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

public final class ChatStage {
    private final String username;
    private final List<ChatMsg> history;
    private Stage primaryStage;
    private TextField serverStatusField, messageSendField, directSendField;
    private TextArea messagesDisplayArea;
    private Client client;
    private final int LIMIT = 1000;

    public ChatStage(String username, List<ChatMsg> history) {
        this.username = username;
        this.history = history;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    private void sendMessage() {
        String message = messageSendField.getText();
        if (!message.equals("")) {
            messageSendField.setText("");
            if (!directSendField.getText().equals("")) {
                sendPrivateMessage(message);
                messageSendField.requestFocus();
            } else {
                client.sendMessage(message);
            }
        }
    }

    private void sendPrivateMessage(String message) {
        String[] receivers = directSendField.getText().split(" ");
        directSendField.setText("");
        client.sendPrivateMessage(receivers, message);
    }


    public void displayLoginMessage(String joiningUsername, long timestamp) {
        String informationText = formattedInformationTextForDisplay(joiningUsername + " joined", timestamp);
        messagesDisplayArea.appendText(informationText + "\n\n");
    }

    public void displayMessage(String sender, String message, long timestamp) {
        String informationText = formattedInformationTextForDisplay(sender, timestamp);
        String messageText = wholeMessageTextForDisplay(informationText, message);
        messagesDisplayArea.appendText(messageText);
    }

    public void displayFailure(String failure, long timestamp) {
        String informationText = formattedInformationTextForDisplay("failure", timestamp);
        String messageText = wholeMessageTextForDisplay(informationText, failure);
        messagesDisplayArea.appendText(messageText);
    }

    public void displayLogoutMessage(String leavingUsername, long timestamp) {
        String informationText = formattedInformationTextForDisplay(leavingUsername + " left", timestamp);
        messagesDisplayArea.appendText(informationText + "\n\n");
    }

    public void displayMessageServerStatus(String status) {
        serverStatusField.setText(status);
    }


    private String formattedInformationTextForDisplay(String information, long timestamp) {
        return getLocalTime(timestamp) + ": " + information;
    }

    private String wholeMessageTextForDisplay(String informationText, String message) {
        return informationText + "\n" + message + "\n\n";
    }

    private String getLocalTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId());
        return (dateTime.getHour() < 10 ? "0" : "") + dateTime.getHour() + ":" + (dateTime.getMinute() < 10 ? "0" : "") + dateTime.getMinute();
    }


    public void display(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(username + "'s chat");

        double width = 450;
        double height = 500;
        this.primaryStage.setX(primaryStage.getX() - (width - primaryStage.getWidth()) / 2);
        this.primaryStage.setY(primaryStage.getY() - (height - primaryStage.getHeight()) / 2);

        this.primaryStage.setOnCloseRequest(e -> {
            e.consume();
            leaveChat();
        });

        messagesDisplayArea = new TextArea();
        messagesDisplayArea.setEditable(false);
        messagesDisplayArea.setPrefHeight(385);
        messagesDisplayArea.setWrapText(true);
        messagesDisplayArea.setFocusTraversable(false);

        ScrollPane scroller = new ScrollPane();
        scroller.setContent(messagesDisplayArea);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        displayHistory();

        messageSendField = new TextField();
        messageSendField.lengthProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue.intValue() > oldValue.intValue()) {
                if (messageSendField.getText().length() >= LIMIT) {
                    messageSendField.setText(messageSendField.getText(0, LIMIT));
                }
            }
        });

        directSendField = new TextField();
        directSendField.setPrefWidth(250);

        serverStatusField = new TextField();
        serverStatusField.setAlignment(Pos.CENTER);
        serverStatusField.setEditable(false);
        serverStatusField.setFocusTraversable(false);
        serverStatusField.setBackground(Background.EMPTY);
        serverStatusField.setBorder(Border.EMPTY);

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());
        sendButton.setDefaultButton(true);

        HBox buttonAndDirectSendFieldLayout = new HBox(119);
        buttonAndDirectSendFieldLayout.getChildren().addAll(sendButton, directSendField);

        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));
        mainLayout.getChildren().addAll(serverStatusField, messagesDisplayArea, messageSendField, buttonAndDirectSendFieldLayout);

        Scene scene = new Scene(mainLayout, width, height);
        this.primaryStage.setScene(scene);

    }


    private void leaveChat() {
        ConfirmBox.display();
        if (ConfirmBox.confirm) {
            primaryStage.close();
            client.logout();
            new LoginStage().start(new Stage());
        }
    }

    private void displayHistory() {
        for (ChatMsg message : history) {
            long timestamp = message.getTime();
            if (message.hasUserSentGlobalMessage()) {
                displayMessage(message.getUserSentGlobalMessage().getUserName(),
                        message.getUserSentGlobalMessage().getMessage(),
                        timestamp);
            } else if (message.hasUserSentPrivateMessage()) {
                displayMessage(message.getUserSentPrivateMessage().getSender(),
                        "pm: " + message.getUserSentPrivateMessage().getMessage(),
                        timestamp);
            } else if (message.hasUserLoggedIn()) {
                displayLoginMessage(message.getUserLoggedIn().getUserName(),
                        timestamp);
            } else if (message.hasUserLoggedOut()) {
                displayLogoutMessage(message.getUserLoggedOut().getUserName(),
                        timestamp);
            } else if (message.hasFailure()) {
                displayFailure(message.getFailure().getMessage(),
                        timestamp);
            } else {
                //do nothing
            }
        }
    }


}
