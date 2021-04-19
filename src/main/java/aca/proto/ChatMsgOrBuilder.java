// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: messages.proto

package aca.proto;

public interface ChatMsgOrBuilder extends
    // @@protoc_insertion_point(interface_extends:aca.proto.ChatMsg)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>required int64 time = 1;</code>
   */
  boolean hasTime();
  /**
   * <code>required int64 time = 1;</code>
   */
  long getTime();

  /**
   * <code>optional string metadata = 42;</code>
   */
  boolean hasMetadata();
  /**
   * <code>optional string metadata = 42;</code>
   */
  java.lang.String getMetadata();
  /**
   * <code>optional string metadata = 42;</code>
   */
  com.google.protobuf.ByteString
      getMetadataBytes();

  /**
   * <code>optional .aca.proto.ChatMsg.ServerStatus serverStatus = 2;</code>
   */
  boolean hasServerStatus();
  /**
   * <code>optional .aca.proto.ChatMsg.ServerStatus serverStatus = 2;</code>
   */
  aca.proto.ChatMsg.ServerStatus getServerStatus();
  /**
   * <code>optional .aca.proto.ChatMsg.ServerStatus serverStatus = 2;</code>
   */
  aca.proto.ChatMsg.ServerStatusOrBuilder getServerStatusOrBuilder();

  /**
   * <code>optional .aca.proto.ChatMsg.UserLoggedIn userLoggedIn = 3;</code>
   */
  boolean hasUserLoggedIn();
  /**
   * <code>optional .aca.proto.ChatMsg.UserLoggedIn userLoggedIn = 3;</code>
   */
  aca.proto.ChatMsg.UserLoggedIn getUserLoggedIn();
  /**
   * <code>optional .aca.proto.ChatMsg.UserLoggedIn userLoggedIn = 3;</code>
   */
  aca.proto.ChatMsg.UserLoggedInOrBuilder getUserLoggedInOrBuilder();

  /**
   * <code>optional .aca.proto.ChatMsg.UserLoggedOut userLoggedOut = 4;</code>
   */
  boolean hasUserLoggedOut();
  /**
   * <code>optional .aca.proto.ChatMsg.UserLoggedOut userLoggedOut = 4;</code>
   */
  aca.proto.ChatMsg.UserLoggedOut getUserLoggedOut();
  /**
   * <code>optional .aca.proto.ChatMsg.UserLoggedOut userLoggedOut = 4;</code>
   */
  aca.proto.ChatMsg.UserLoggedOutOrBuilder getUserLoggedOutOrBuilder();

  /**
   * <code>optional .aca.proto.ChatMsg.UserSentGlobalMessage userSentGlobalMessage = 5;</code>
   */
  boolean hasUserSentGlobalMessage();
  /**
   * <code>optional .aca.proto.ChatMsg.UserSentGlobalMessage userSentGlobalMessage = 5;</code>
   */
  aca.proto.ChatMsg.UserSentGlobalMessage getUserSentGlobalMessage();
  /**
   * <code>optional .aca.proto.ChatMsg.UserSentGlobalMessage userSentGlobalMessage = 5;</code>
   */
  aca.proto.ChatMsg.UserSentGlobalMessageOrBuilder getUserSentGlobalMessageOrBuilder();

  /**
   * <code>optional .aca.proto.ChatMsg.UserSentPrivateMessage userSentPrivateMessage = 7;</code>
   */
  boolean hasUserSentPrivateMessage();
  /**
   * <code>optional .aca.proto.ChatMsg.UserSentPrivateMessage userSentPrivateMessage = 7;</code>
   */
  aca.proto.ChatMsg.UserSentPrivateMessage getUserSentPrivateMessage();
  /**
   * <code>optional .aca.proto.ChatMsg.UserSentPrivateMessage userSentPrivateMessage = 7;</code>
   */
  aca.proto.ChatMsg.UserSentPrivateMessageOrBuilder getUserSentPrivateMessageOrBuilder();

  /**
   * <code>optional .aca.proto.ChatMsg.Failure failure = 6;</code>
   */
  boolean hasFailure();
  /**
   * <code>optional .aca.proto.ChatMsg.Failure failure = 6;</code>
   */
  aca.proto.ChatMsg.Failure getFailure();
  /**
   * <code>optional .aca.proto.ChatMsg.Failure failure = 6;</code>
   */
  aca.proto.ChatMsg.FailureOrBuilder getFailureOrBuilder();

  public aca.proto.ChatMsg.MsgCase getMsgCase();
}