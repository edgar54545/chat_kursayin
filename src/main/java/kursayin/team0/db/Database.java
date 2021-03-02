package kursayin.team0.db;

import aca.proto.ChatMsg;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class Database {
    private Connection connection;

    private Comparator<ChatMsg> comparator = Comparator.comparingLong(ChatMsg::getTime);

    public void createConnection() throws SQLException {
        connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/kursayin", "root", "root");
    }

    public boolean isUsernameFree(String username) throws SQLException {
        ResultSet resultSet = connection.createStatement()
                .executeQuery("SELECT * FROM users WHERE username = '" + username + "'");
        if (resultSet.next() && resultSet.getBoolean("is_active")) {
            return false;
        }
        return true;
    }

    public List<ChatMsg> getPreviousConversation(String username) throws SQLException {
        List<ChatMsg> wholeHistory = getLoginHistory();
        wholeHistory.addAll(getGlobalMessageHistory());
        wholeHistory.addAll(getPrivateMessageHistory(username));
        wholeHistory.addAll(getLogoutHistory());
        wholeHistory.addAll(getFailureHistory());
        wholeHistory.sort(comparator);
        return wholeHistory;
    }

    public void addLogin(ChatMsg loginMessage) throws SQLException, IllegalArgumentException {
        if (!loginMessage.hasUserLoggedIn()) {
            throw new IllegalArgumentException();
        }
        String username = loginMessage.getUserLoggedIn().getUserName();
        if (connection.createStatement().executeQuery("SELECT * FROM users WHERE username = '" + username + "'").next()) {
            connection.createStatement().execute("UPDATE users SET is_active = 1 WHERE username = '" + username + "'");
        } else {
            connection.createStatement().execute("INSERT INTO users VALUES ('" + username + "', 1);");
        }
        long timestamp = loginMessage.getTime();
        connection.createStatement().execute("INSERT INTO logins VALUES (NULL, '" + username + "', " + timestamp + ");");
    }

    public void addLogout(ChatMsg logoutMessage) throws SQLException, IllegalArgumentException {
        if (!logoutMessage.hasUserLoggedOut()) {
            throw new IllegalArgumentException();
        }
        String username = logoutMessage.getUserLoggedOut().getUserName();
        connection.createStatement().execute("UPDATE users SET is_active = 0 WHERE username = '" + username + "'");
        long timestamp = logoutMessage.getTime();
        connection.createStatement().execute("INSERT INTO logouts VALUES (NULL, '" + username + "', " + timestamp + ");");
    }

    public void addGlobalMessage(ChatMsg globalMessage) throws SQLException, IllegalArgumentException {
        if (!globalMessage.hasUserSentGlobalMessage()) {
            throw new IllegalArgumentException();
        }
        String username = globalMessage.getUserSentGlobalMessage().getUserName();
        String message = globalMessage.getUserSentGlobalMessage().getMessage();
        long timestamp = globalMessage.getTime();
        connection.createStatement().execute("INSERT INTO global_messages VALUES (NULL, '" +
                username + "', '" + message + "', " + timestamp + ");");
    }

    public void addPrivateMessage(ChatMsg privateMessage) throws SQLException, IllegalArgumentException {
        if (!privateMessage.hasUserSentPrivateMessage()) {
            throw new IllegalArgumentException();
        }
        String username = privateMessage.getUserSentPrivateMessage().getSender();
        List<String> receivers = privateMessage.getUserSentPrivateMessage().getReceiverList();
        String message = privateMessage.getUserSentPrivateMessage().getMessage();
        long timestamp = privateMessage.getTime();
        connection.createStatement().execute("INSERT INTO private_messages VALUES (NULL, '" + username +
                "', '" + String.join(" ", receivers) + "', '" + message + "', " + timestamp + ");");
    }

    public void addFailure(ChatMsg failureMessage) throws SQLException, IllegalArgumentException {
        if (!failureMessage.hasFailure()) {
            throw new IllegalArgumentException();
        }
        String failure = failureMessage.getFailure().getMessage();
        long timestamp = failureMessage.getTime();
        connection.createStatement().execute("INSERT INTO failures VALUES (NULL, '" + failure + "', " + timestamp + ");");
    }


    private List<ChatMsg> getLoginHistory() throws SQLException {
        List<ChatMsg> logins = new ArrayList<>();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT username, time FROM logins;");
        while (resultSet.next()) {
            ChatMsg login = ChatMsg.newBuilder()
                    .setTime(resultSet.getLong("time"))
                    .setUserLoggedIn(ChatMsg.UserLoggedIn.newBuilder()
                            .setUserName(resultSet.getString("username")))
                    .build();
            logins.add(login);
        }
        return logins;
    }

    private List<ChatMsg> getLogoutHistory() throws SQLException {
        List<ChatMsg> logouts = new ArrayList<>();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT username, time FROM logouts;");
        while (resultSet.next()) {
            ChatMsg logout = ChatMsg.newBuilder()
                    .setTime(resultSet.getLong("time"))
                    .setUserLoggedOut(ChatMsg.UserLoggedOut.newBuilder()
                            .setUserName(resultSet.getString("username")))
                    .build();
            logouts.add(logout);
        }
        return logouts;
    }

    private List<ChatMsg> getGlobalMessageHistory() throws SQLException {
        List<ChatMsg> globalMessages = new ArrayList<>();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT sender, message, time FROM global_messages;");
        while (resultSet.next()) {
            ChatMsg globalMessage = ChatMsg.newBuilder()
                    .setTime(resultSet.getLong("time"))
                    .setUserSentGlobalMessage(ChatMsg.UserSentGlobalMessage.newBuilder()
                            .setUserName(resultSet.getString("sender"))
                            .setMessage(resultSet.getString("message")))
                    .build();
            globalMessages.add(globalMessage);
        }
        return globalMessages;
    }

    private List<ChatMsg> getPrivateMessageHistory(String username) throws SQLException {
        List<ChatMsg> privateMessages = new ArrayList<>();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM private_messages;");
        while (resultSet.next()) {
            String[] receivers = resultSet.getString("receivers").split(" ");
            Iterable<String> allReceivers = Arrays.asList(receivers);
            for (String receiver : receivers) {
                if (receiver.equals(username)) {
                    ChatMsg privateMessage = ChatMsg.newBuilder()
                            .setTime(resultSet.getLong("time"))
                            .setUserSentPrivateMessage(ChatMsg.UserSentPrivateMessage.newBuilder()
                                    .setSender(resultSet.getString("sender"))
                                    .setMessage(resultSet.getString("message"))
                                    .addAllReceiver(allReceivers))
                            .build();
                    privateMessages.add(privateMessage);
                }
            }
        }
        return privateMessages;
    }

    private List<ChatMsg> getFailureHistory() throws SQLException {
        List<ChatMsg> failures = new ArrayList<>();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT failure, time FROM failures;");
        while (resultSet.next()) {
            ChatMsg failure = ChatMsg.newBuilder()
                    .setTime(resultSet.getLong("time"))
                    .setFailure(ChatMsg.Failure.newBuilder()
                            .setMessage(resultSet.getString("failure")))
                    .build();
            failures.add(failure);
        }
        return failures;
    }

}
