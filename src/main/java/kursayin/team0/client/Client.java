package kursayin.team0.client;

import aca.proto.ChatMsg;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kursayin.team0.db.Database;
import kursayin.team0.frames.ChatStage;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.channels.SelectionKey.*;

public class Client implements Runnable {
    private final Database database;
    private final ChatStage chatStage;
    private final String username;
    private static final Logger LOG = LoggerFactory.getLogger(Client.class);
    private SocketChannel socketChannel;
    private Selector selector;
    private ByteBuffer msgToRead = ByteBuffer.allocate(1035);
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private final InetSocketAddress inetSocketAddress;
    private ByteBuffer msgToWrite = ByteBuffer.allocate(1035);

    public Client(String host, int port, String username, ChatStage chatStage, Database database) {

        InetAddress in;
        try {
            in = InetAddress.getByName(host);
        } catch (UnknownHostException un) {
            LOG.error("Unknown Host " + host);
            throw new IllegalStateException("Illegal host " + host);
        }
        inetSocketAddress = new InetSocketAddress(in, port);
        this.username = username;
        this.chatStage = chatStage;
        this.database = database;
    }


    @Override
    public void run() {
        try (SocketChannel client = SocketChannel.open()) {
            client.configureBlocking(false);
            selector = Selector.open();
            client.register(selector, OP_CONNECT);
            client.connect(inetSocketAddress);

            while (!Thread.currentThread().isInterrupted()) {
                int selectionKeysCount = selector.select(100);
                if (selectionKeysCount > 0) {

                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> it = selectedKeys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        socketChannel = (SocketChannel) key.channel();

                        if (!key.isValid()) {
                            continue;
                        }

                        if (key.isConnectable()) {
                            try {
                                while (client.isConnectionPending()) {
                                    client.finishConnect();
                                }
                                LOG.info("connected");
                                login();
                                key.channel().register(key.selector(), OP_READ);
                            } catch (IOException e) {
                                key.cancel();
                                throw e;
                            }
                        } else if (key.isReadable()) {
                            read(key);
                        }
                        it.remove();
                    }
                }
            }
        } catch (EOFException eo) {
            close();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Logout force");
            }
        } catch (IOException io) {
            LOG.error("IOexception " + io);
        } finally {
            close();
        }
    }

    private void close() {
        try {
            socketChannel.close();
        } catch (IOException io) {
            LOG.error("Can't close socketChannel: ", io);
        }
    }

    private void read(SelectionKey key) {
        try {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            socketChannel.read(msgToRead);
            msgToRead.flip();
            while (true) {
                ChatMsg chatMsg;
                try {
                    chatMsg = deserialize(msgToRead);
                } catch (RuntimeException r) {
                    LOG.error("Incorrect message type ", r);
                    break;
                }

                if (chatMsg == null) {
                    break;
                }
                handleMsg(chatMsg);
            }
            msgToRead.compact();
        } catch (EOFException eo) {
            logout();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Logout force");
            }
        } catch (IOException io) {
            LOG.error("Can't read message " + io);
        }
    }

    public void sendMessage(String message) {
        if (message.length() > 1000) {
            LOG.error("Message must be less than 1000 characters");
            return;
        }
        ChatMsg chatMsg = ChatMsg.newBuilder()
                .setTime(System.currentTimeMillis())
                .setUserSentGlobalMessage(
                        ChatMsg.UserSentGlobalMessage.newBuilder()
                                .setMessage(message)
                                .setUserName(username)
                                .build())
                .build();
        boolean success = serialize(chatMsg, msgToWrite);
        msgToWrite.flip();
        if (success) {
            try {
                database.addGlobalMessage(chatMsg);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            sendToServer(msgToWrite);
        } else {
            LOG.error("Can't send message: " + message);
        }
    }

    public void login() {
        ChatMsg chatMsg = ChatMsg.newBuilder()
                .setTime(System.currentTimeMillis())
                .setUserLoggedIn(ChatMsg.UserLoggedIn.newBuilder().setUserName(username))
                .build();

        boolean success = serialize(chatMsg, msgToWrite);
        msgToWrite.flip();
        if (success) {
            try {
                database.addLogin(chatMsg);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            sendToServer(msgToWrite);
        } else {
            LOG.error("Can't send login message");
        }
    }


    public void logout() {

        ChatMsg chatMsg = ChatMsg.newBuilder()
                .setTime(System.currentTimeMillis())
                .setUserLoggedOut(ChatMsg.UserLoggedOut.newBuilder().setUserName(username))
                .build();
        boolean success = serialize(chatMsg, msgToWrite);
        msgToWrite.flip();
        if (success) {
            try {
                database.addLogout(chatMsg);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            sendToServer(msgToWrite);
        } else {
            LOG.error("Can't send logout message");
        }
        close();
    }

    private void sendToServer(ByteBuffer buffer) {
        try {
            socketChannel.write(buffer);
            socketChannel.register(selector, OP_READ);
        } catch (EOFException eo) {
            logout();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Logout force");
            }
        } catch (IOException io) {
            LOG.error("Unable to send " + username + "'s message " + io);
        }
    }

    private void handleMsg(ChatMsg chatMsg) {

        if (chatMsg.hasUserSentGlobalMessage()) {
            if (chatMsg.getUserSentGlobalMessage().getMessage().length() > 1000) {
                LOG.error("Message length is more than 1000 ");
            } else {
                executor.submit(() -> chatStage.displayMessage(chatMsg.getUserSentGlobalMessage().getUserName(),
                        chatMsg.getUserSentGlobalMessage().getMessage(), System.currentTimeMillis()));
                LOG.info(chatMsg.getUserSentGlobalMessage().getUserName() + ": " + chatMsg.getUserSentGlobalMessage().getMessage());
            }
        } else if (chatMsg.hasFailure()) {
            LOG.info(chatMsg.getFailure().getMessage());
            executor.submit(() -> chatStage.displayFailure(chatMsg.getFailure().getMessage(), chatMsg.getTime()));
        } else if (chatMsg.hasUserLoggedOut()) {
            if (chatMsg.getUserLoggedOut().getUserName().equals(username)) {
                logout();
            } else {
                executor.submit(() -> chatStage.displayLogoutMessage(chatMsg.getUserLoggedOut().getUserName(), chatMsg.getTime()));
                LOG.info(chatMsg.getUserLoggedOut().getUserName() + " left");
            }
        } else if (chatMsg.hasUserLoggedIn()) {
            LOG.info(chatMsg.getUserLoggedIn().getUserName() + " join");
            executor.submit(() -> chatStage.displayLoginMessage(chatMsg.getUserLoggedIn().getUserName(), chatMsg.getTime()));
        } else if (chatMsg.hasUserSentPrivateMessage()) {
            List<String> names = chatMsg.getUserSentPrivateMessage().getReceiverList();
            if (names.contains(username)) {
                executor.submit(() -> chatStage.displayMessage(chatMsg.getUserSentPrivateMessage().getSender(),
                        "pm: " + chatMsg.getUserSentPrivateMessage().getMessage(),
                        chatMsg.getTime()));

                LOG.info(chatMsg.getUserSentPrivateMessage().getSender() + " pm: " + chatMsg.getUserSentPrivateMessage().getMessage());
            }
            //}
        } else if (chatMsg.hasServerStatus()) {
            executor.submit(() -> chatStage.displayMessageServerStatus(chatMsg.getServerStatus().getStatus()));
        }
    }

    public void sendPrivateMessage(String[] receivers, String message) {

        String[] receiverAndSender = new String[receivers.length + 1];
        for (int i = 0; i < receivers.length; i++) {
            receiverAndSender[i] = receivers[i];
        }
        receiverAndSender[receiverAndSender.length - 1] = username;
        Iterable<String> allReceivers = Arrays.asList(receiverAndSender);

        ChatMsg chatMsg = ChatMsg.newBuilder()
                .setTime(System.currentTimeMillis())
                .setUserSentPrivateMessage(ChatMsg.UserSentPrivateMessage.newBuilder()
                        .setSender(username)
                        .setMessage(message)
                        .addAllReceiver(allReceivers))
                .build();
        msgToWrite.clear();
        boolean success = serialize(chatMsg, msgToWrite);
        msgToWrite.flip();
        if (success) {
            try {
                database.addPrivateMessage(chatMsg);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            sendToServer(msgToWrite);
        } else {
            LOG.error("Can't send message: " + message);
        }
    }

    private boolean serialize(ChatMsg chatMsg, ByteBuffer buffer) {
        buffer.clear();
        byte[] msgByteArr = chatMsg.toByteArray();
        int msgbytes = 4 + msgByteArr.length;

        if (buffer.remaining() < msgbytes) {
            return false;
        }

        buffer.putInt(msgByteArr.length);
        buffer.put(msgByteArr);

        return true;
    }

    private ChatMsg deserialize(ByteBuffer buffer) {
        if (buffer.remaining() <= 4) {
            return null;
        }

        buffer.mark();
        int length = buffer.getInt();

        if (buffer.remaining() < length) {
            buffer.reset();
            return null;
        }

        byte[] bytes = new byte[length];
        buffer.get(bytes);

        try {
            return ChatMsg.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);

        }
    }
}
