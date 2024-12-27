package cs451;

import java.io.*;
import java.util.*;

public class Message implements Serializable {
    public enum MessageType {
        PROPOSAL,
        ACK,
        NACK
    }

    private final MessageType type;
    private final int senderId;
    private final int proposalId;
    private final int proposalNb;
    private final Set<Integer> proposalSet;

    // Constructor
    private Message(MessageType type, int senderId, int proposalId, int proposalNb, Set<Integer> proposalSet) {
        if (type == null) {
            throw new IllegalArgumentException("Message type cannot be null");
        }
        if (senderId <= 0) {
            throw new IllegalArgumentException("Sender ID must be positive");
        }
        if (proposalId < 0) {
            throw new IllegalArgumentException("Proposal ID cannot be negative");
        }
        if (proposalNb < 0) {
            throw new IllegalArgumentException("Proposal number cannot be negative");
        }
        if ((type == MessageType.PROPOSAL || type == MessageType.NACK) && (proposalSet == null || proposalSet.isEmpty())) {
            throw new IllegalArgumentException("Proposal set cannot be null or empty for PROPOSAL or NACK messages");
        }

        this.type = type;
        this.senderId = senderId;
        this.proposalId = proposalId;
        this.proposalNb = proposalNb;
        this.proposalSet = proposalSet != null ? new HashSet<>(proposalSet) : null;
    }

    // Static factory methods with validation
    public static Message createProposal(int senderId, int proposalId, Set<Integer> proposalSet, int proposalNb) {
        return new Message(MessageType.PROPOSAL, senderId, proposalId, proposalNb, proposalSet);
    }

    public static Message createAck(int senderId, int proposalId, int proposalNb) {
        return new Message(MessageType.ACK, senderId, proposalId, proposalNb, null);
    }

    public static Message createNoAck(int senderId, int proposalId, int proposalNb, Set<Integer> proposalSet) {
        return new Message(MessageType.NACK, senderId, proposalId, proposalNb, proposalSet);
    }

    // Getters
    public MessageType getType() {
        return type;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getProposalId() {
        return proposalId;
    }

    public int getProposalNb() {
        return proposalNb;
    }

    public Set<Integer> getProposalSet() {
        return proposalSet != null ? new HashSet<>(proposalSet) : null;
    }

    // Serialization to byte array (custom serialization for efficiency)
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DataOutputStream dataStream = new DataOutputStream(byteStream)) {
            dataStream.writeInt(type.ordinal()); // Serialize MessageType as an integer
            dataStream.writeInt(senderId);
            dataStream.writeInt(proposalId);
            dataStream.writeInt(proposalNb);

            // Serialize proposalSet
            if (proposalSet != null) {
                dataStream.writeInt(proposalSet.size());
                for (int value : proposalSet) {
                    dataStream.writeInt(value);
                }
            } else {
                dataStream.writeInt(0); // Indicate empty set
            }
        }
        return byteStream.toByteArray();
    }

    // Deserialization from byte array (custom deserialization for efficiency)
    public static Message fromBytes(byte[] bytes) throws IOException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        try (DataInputStream dataStream = new DataInputStream(byteStream)) {
            MessageType type = MessageType.values()[dataStream.readInt()];
            int senderId = dataStream.readInt();
            int proposalId = dataStream.readInt();
            int proposalNb = dataStream.readInt();

            // Deserialize proposalSet
            int setSize = dataStream.readInt();
            Set<Integer> proposalSet = new HashSet<>();
            for (int i = 0; i < setSize; i++) {
                proposalSet.add(dataStream.readInt());
            }

            return new Message(type, senderId, proposalId, proposalNb, proposalSet.isEmpty() ? null : proposalSet);
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", senderId=" + senderId +
                ", proposalId=" + proposalId +
                ", proposalNb=" + proposalNb +
                ", proposalSet=" + proposalSet +
                '}';
    }
}
