package kursayin.team0;

import aca.proto.ChatMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.imageio.IIOException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import static kursayin.team0.util.Util.*;
import static java.nio.channels.SelectionKey.*;


public class NonBlockingServer {


    private static final Logger LOG = LoggerFactory.getLogger(NonBlockingServer.class);
    private static final List<SocketChannel> channels = new ArrayList<>();
    private static final HashMap<String, SocketChannel> channelsMap = new HashMap<>();
    private static List<String> directReceivers;

    public static void main(String[] args) throws Exception {

        LOG.info("Waiting for a client");


        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.configureBlocking(false);

            Selector selector = Selector.open();
            InetSocketAddress hostAddress = new InetSocketAddress("localhost", 9001);
            serverChannel.socket().setReuseAddress(true);
            serverChannel.bind(hostAddress);

            serverChannel.register(selector, OP_ACCEPT);

            ChatMsg response = null;
            while (!Thread.currentThread().isInterrupted()) {
                int selectedKeys = selector.select();
                if (selectedKeys > 0) {

                    Set<SelectionKey> readySet = selector.selectedKeys();

                    Iterator<SelectionKey> iterator = readySet.iterator();

                    while (iterator.hasNext()) {

                        SelectionKey key = iterator.next();

                        if (key.isAcceptable()) {
                            SocketChannel socketChannel = serverChannel.accept();
                            socketChannel.configureBlocking(false);
                            ChatMsg serverStatus = ChatMsg.newBuilder()
                                    .setTime(System.currentTimeMillis())
                                    .setServerStatus(ChatMsg.ServerStatus.newBuilder().setStatus("Server has been created by team0 (Minas, Edgar, Arpine)").build())
                                    .build();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            write(buffer, serverStatus);
                            buffer.flip();
                            socketChannel.write(buffer);
                            socketChannel.register(selector, OP_READ);
                            LOG.info("Client connected:" + hostAddress);
                            channels.add(socketChannel);
                        } else if (key.isReadable()) {
                            LOG.debug("reading");
                            SocketChannel socketChannel = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            try {
                                socketChannel.read(buffer);
                                buffer.flip();
                                ChatMsg message = read(buffer);

                                if (message != null) {
                                    if (message.hasUserLoggedIn()) {
                                        channelsMap.put(message.getUserLoggedIn().getUserName(), socketChannel);
                                    }

                                    buffer.compact();
                                    LOG.debug("<< " + message);

                                    response = message;
                                    socketChannel.register(selector, OP_WRITE);
                                } else {
                                    socketChannel.register(selector, OP_WRITE);
                                    continue;
                                }
                            } catch (IIOException e) {
                                socketChannel.close();
                                key.cancel();
                            }

                        } else if (key.isWritable()) {
                            LOG.debug("writing");
                            SocketChannel socketChannel = (SocketChannel) key.channel();
//                                channel = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);

                            if (response != null) {
                                if (response.hasServerStatus()) {
                                    LOG.info("<< The server status is:" + response.getServerStatus().getStatus());
                                } else if (response.hasUserLoggedIn()) {
                                    LOG.info("<< " + response.getUserLoggedIn().getUserName() + " has logged in:" + response.getTime());
                                } else if (response.hasUserLoggedOut()) {
                                    LOG.info("<< " + response.getUserLoggedOut().getUserName() + " has logged out:" + response.getTime());
                                    socketChannel.close();
                                    channels.remove(socketChannel);
                                    channelsMap.remove(response.getUserLoggedOut().getUserName());
                                } else if (response.hasUserSentGlobalMessage()) {
                                    LOG.info("<< " + response.getUserSentGlobalMessage().getUserName() + ": "
                                            + response.getUserSentGlobalMessage().getMessage());
                                } else if (response.hasFailure()) {
                                    LOG.error("<< " + response.getFailure().getMessage());
                                } else if (response.hasUserSentPrivateMessage()) {
                                    LOG.info(response.getUserSentPrivateMessage().getSender() + "pm: " +
                                            response.getUserSentPrivateMessage().getMessage());
                                    directReceivers = response.getUserSentPrivateMessage().getReceiverList();
                                    LOG.info("receivers: " + directReceivers);

                                }

                                write(buffer, response);
                                buffer.flip();

                                if (!response.hasUserSentPrivateMessage()) {
                                    for (SocketChannel channel : channels) {
                                        if (channel.isConnected()) {
                                            channel.write(buffer);
                                            channel.register(selector, OP_READ);
                                            buffer.rewind();
                                        }
                                    }
                                } else if (response.hasUserSentPrivateMessage()) {
                                    for (String username : channelsMap.keySet()) {
                                        if (directReceivers.contains(username)) {
                                            SocketChannel channel = channelsMap.get(username);
                                            if (channel.isConnected()) {
                                                try {
                                                    LOG.debug("pm");
                                                    channel.write(buffer);
                                                    channel.register(selector, OP_READ);
                                                    buffer.rewind();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                    buffer.clear();
                                }
                            } else {
                                for (SocketChannel channel : channels) {
                                    channel.register(selector, OP_READ);
                                }
                                continue;
                            }
                        }
                        iterator.remove();
                    }
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (ClosedChannelException e) {
            LOG.warn("a channel is closed");
        }
    }
}
