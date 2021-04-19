package kursayin.team0.db;

import aca.proto.ChatMsg;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class Database {
    private Connection connection;
    private Cluster cluster;
    private Session cassandraSession;

    private Comparator<ChatMsg> comparator = Comparator.comparingLong(ChatMsg::getTime);

    public void createConnection() throws SQLException {
        connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/kursayin", "root", "root");
        cluster = Cluster.builder()
                .withClusterName("cassandra")
                .addContactPoint("127.0.0.1")
                .build();

        cassandraSession = cluster.connect("kursayin");
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
        cassandraSession.execute("INSERT INTO logins (username, time) VALUES ('" + username + "', " + timestamp + ");");
    }

    public void addLogout(ChatMsg logoutMessage) throws SQLException, IllegalArgumentException {
        if (!logoutMessage.hasUserLoggedOut()) {
            throw new IllegalArgumentException();
        }
        String username = logoutMessage.getUserLoggedOut().getUserName();
        connection.createStatement().execute("UPDATE users SET is_active = 0 WHERE username = '" + username + "'");
        long timestamp = logoutMessage.getTime();
        cassandraSession.execute("INSERT INTO logouts (username, time) VALUES ('" + username + "', " + timestamp + ");");
    }

    public void addGlobalMessage(ChatMsg globalMessage) throws SQLException, IllegalArgumentException {
        if (!globalMessage.hasUserSentGlobalMessage()) {
            throw new IllegalArgumentException();
        }
        String username = globalMessage.getUserSentGlobalMessage().getUserName();
        String message = globalMessage.getUserSentGlobalMessage().getMessage();
        long timestamp = globalMessage.getTime();

        cassandraSession.execute("INSERT INTO global_messages (username, message, time) VALUES ('" +
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
        cassandraSession.execute("INSERT INTO private_messages (username, receivers, message, time) VALUES ('" + username +
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
        com.datastax.driver.core.ResultSet rows =
                cassandraSession.execute("SELECT username, time FROM logins;");
        for (Row row : rows) {
            ChatMsg login = ChatMsg.newBuilder()
                    .setTime(row.getLong("time"))
                    .setUserLoggedIn(ChatMsg.UserLoggedIn.newBuilder()
                            .setUserName(row.getString("username")))
                    .build();
            logins.add(login);
        }
        return logins;
    }

    private List<ChatMsg> getLogoutHistory() throws SQLException {
        List<ChatMsg> logouts = new ArrayList<>();
        com.datastax.driver.core.ResultSet rows = cassandraSession.execute("SELECT username, time FROM logouts;");
        for (Row row : rows) {
            ChatMsg logout = ChatMsg.newBuilder()
                    .setTime(row.getLong("time"))
                    .setUserLoggedOut(ChatMsg.UserLoggedOut.newBuilder()
                            .setUserName(row.getString("username")))
                    .build();
            logouts.add(logout);
        }
        return logouts;
    }

    private List<ChatMsg> getGlobalMessageHistory() throws SQLException {
        List<ChatMsg> globalMessages = new ArrayList<>();
        com.datastax.driver.core.ResultSet rows =
                cassandraSession.execute("SELECT username, message, time FROM global_messages;");
        for (Row row : rows) {
            ChatMsg globalMessage = ChatMsg.newBuilder()
                    .setTime(row.getLong("time"))
                    .setUserSentGlobalMessage(ChatMsg.UserSentGlobalMessage.newBuilder()
                            .setUserName(row.getString("username"))
                            .setMessage(row.getString("message")))
                    .build();
            globalMessages.add(globalMessage);
        }
        return globalMessages;
    }

    private List<ChatMsg> getPrivateMessageHistory(String username) throws SQLException {
        List<ChatMsg> privateMessages = new ArrayList<>();
        com.datastax.driver.core.ResultSet rows = cassandraSession.execute("SELECT * FROM private_messages;");
        for (Row row : rows) {
            String[] receivers = row.getString("receivers").split(" ");
            Iterable<String> allReceivers = Arrays.asList(receivers);
            for (String receiver : receivers) {
                if (receiver.equals(username)) {
                    ChatMsg privateMessage = ChatMsg.newBuilder()
                            .setTime(row.getLong("time"))
                            .setUserSentPrivateMessage(ChatMsg.UserSentPrivateMessage.newBuilder()
                                    .setSender(row.getString("username"))
                                    .setMessage(row.getString("message"))
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
        com.datastax.driver.core.ResultSet rows =
                cassandraSession.execute("SELECT failure, time FROM failures;");
        for (Row row : rows) {
            ChatMsg failure = ChatMsg.newBuilder()
                    .setTime(row.getLong("time"))
                    .setFailure(ChatMsg.Failure.newBuilder()
                            .setMessage(row.getString("failure")))
                    .build();
            failures.add(failure);
        }
        return failures;
    }

}
