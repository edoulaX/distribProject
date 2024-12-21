package cs451;

import java.io.*;
import java.util.*;

public class Message implements Serializable {
    private final int senderId; // Current sender's ID
    private final int seqNo; // Sequence number of the message
    private final int originalSenderId; // Original sender's ID
    private final String content; // Message content
    private final boolean isAck; // Is this an acknowledgment message?
    private final Set<Integer> ackList; // List of processes that have acknowledged this message
    private final Set<Integer> latticeState; // Represents the lattice state

    // Constructor for normal messages
    public Message(int senderId, int seqNo, int originalSenderId, String content, boolean isAck, Set<Integer> ackList, Set<Integer> latticeState) {
        this.senderId = senderId;
        this.seqNo = seqNo;
        this.originalSenderId = originalSenderId;
        this.content = content;
        this.isAck = isAck;
        this.ackList = Collections.unmodifiableSet(new HashSet<>(ackList)); // Immutable set
        this.latticeState = Collections.unmodifiableSet(new HashSet<>(latticeState)); // Immutable set
    }

    // Getters
    public int getSenderId() {
        return senderId;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public int getOriginalSenderId() {
        return originalSenderId;
    }

    public String getContent() {
        return content;
    }

    public boolean isAck() {
        return isAck;
    }

    public Set<Integer> getAckList() {
        return ackList; // Return the immutable set
    }

    public Set<Integer> getLatticeState() {
        return latticeState;
    }

    public String getId() {
        return originalSenderId + "-" + seqNo; // Unique ID based on original sender and sequence number
    }

    // Serialization
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(this);
        out.flush();
        return bos.toByteArray();
    }

    public static Message fromBytes(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream in = new ObjectInputStream(bis);
        return (Message) in.readObject();
    }

    // Factory method for acknowledgment messages
    public static Message createAck(int senderId, int seqNo, int originalSenderId, Set<Integer> ackList) {
        return new Message(senderId, seqNo, originalSenderId, null, true, ackList, new HashSet<>());
    }

    // Factory method to create a new message with an updated acknowledgment list
    public static Message withUpdatedAckList(Message message, Set<Integer> updatedAckList) {
        return new Message(
                message.getSenderId(),
                message.getSeqNo(),
                message.getOriginalSenderId(),
                message.getContent(),
                message.isAck(),
                updatedAckList,
                message.getLatticeState()
        );
    }

    // Factory method to merge lattice states
    public static Message withMergedLatticeState(Message message, Set<Integer> updatedLatticeState) {
        Set<Integer> mergedState = new HashSet<>(message.getLatticeState());
        mergedState.addAll(updatedLatticeState);
        return new Message(
                message.getSenderId(),
                message.getSeqNo(),
                message.getOriginalSenderId(),
                message.getContent(),
                message.isAck(),
                message.getAckList(),
                mergedState
        );
    }

    // Utility method to create a new message with an updated lattice state
    public static Message createLatticeMessage(int senderId, int seqNo, int originalSenderId, String content, Set<Integer> latticeState) {
        return new Message(senderId, seqNo, originalSenderId, content, false, new HashSet<>(), latticeState);
    }
}
