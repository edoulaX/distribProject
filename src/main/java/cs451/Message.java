package cs451;

import java.io.*;

public class Message implements Serializable {
    private final int senderId;
    private final int seqNo;
    private final String content;
    private final boolean isAck;

    public Message(int senderId, int seqNo, String content, boolean isAck) {
        this.senderId = senderId;
        this.seqNo = seqNo;
        this.content = content;
        this.isAck = isAck;
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
        return new Message(senderId, seqNo, null, true);
    }
}
