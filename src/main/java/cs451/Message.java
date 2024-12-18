package cs451;

import java.io.*;
import java.util.*;

public class Message implements Serializable {
    private final int senderId;
    private final int seqNo;
    private final String content;
    private final boolean isAck;
    private final Set<Integer> proposalSet; // New field for proposals or decisions
    private final boolean isNoAck; // New field for NoAck messages

    // Constructor with proposalSet and NoAck flag
    public Message(int senderId, int seqNo, String content, boolean isAck, Set<Integer> proposalSet, boolean isNoAck) {
        this.senderId = senderId;
        this.seqNo = seqNo;
        this.content = content;
        this.isAck = isAck;
        this.isNoAck = isNoAck;
        this.proposalSet = proposalSet == null ? new HashSet<>() : new HashSet<>(proposalSet);
    }

    // Simplified constructor for non-lattice messages
    public Message(int senderId, int seqNo, String content, boolean isAck) {
        this(senderId, seqNo, content, isAck, null, false);
    }

    public int getSenderId() {
        return senderId;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public String getContent() {
        return content;
    }

    public boolean isAck() {
        return isAck;
    }

    public boolean isNoAck() {
        return isNoAck;
    }

    public Set<Integer> getProposalSet() {
        return Collections.unmodifiableSet(proposalSet);
    }

    public String getId() {
        return senderId + "-" + seqNo;
    }

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

    public static Message createAck(int senderId, int seqNo) {
        return new Message(senderId, seqNo, null, true, null, false);
    }

    public static Message createNoAck(int senderId, int seqNo, Set<Integer> proposalSet) {
        return new Message(senderId, seqNo, null, false, proposalSet, true);
    }

    @Override
    public String toString() {
        return "Message{" +
                "senderId=" + senderId +
                ", seqNo=" + seqNo +
                ", content='" + content + '\'' +
                ", isAck=" + isAck +
                ", isNoAck=" + isNoAck +
                ", proposalSet=" + proposalSet +
                '}';
    }
}