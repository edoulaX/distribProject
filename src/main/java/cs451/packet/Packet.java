package cs451.packet;

import cs451.Host;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class Packet {

    private PacketTypes type;
    private int seqNr;
    private int senderId;

    public Packet(PacketTypes type, int seqNr, int senderId) {
        this.type = type;
        this.seqNr = seqNr;
        this.senderId = senderId;
    }

    public Packet(PacketTypes type, DatagramPacket from) {
        String msg = new String(from.getData());
        this.type = type;
        this.seqNr = Integer.parseInt(msg.split("-")[0].trim());
        this.senderId = Host.portToId.get(from.getPort());
    }

    // Converts this Packet into a DatagramPacket for network transmission
    public DatagramPacket getDatagram() {
        String payload = seqNr + "-";
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(bytes, bytes.length);
    }

    public String getMsg() {
        return type + " " + senderId + " " + seqNr;
    }

    public String getFileLine() {
        return type.getFileLine(this);
    }

    public int getSeqNr() {
        return seqNr;
    }

    public int getSenderId() {
        return senderId;
    }

    @Override
    public String toString() {
        return "Packet{" + "type=" + type + ", seqNr=" + seqNr + ", senderId=" + senderId + '}';
    }

}


