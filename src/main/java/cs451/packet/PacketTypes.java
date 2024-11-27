package cs451.packet;

public enum PacketTypes {

    BROADCAST('b', (p) -> p.getSeqNr() + ""),        // Broadcasting packets with just the sequence number
    DELIVER('d', (p) -> p.getSenderId() + " " + p.getSeqNr()),  // Deliver packets including sender ID and sequence number
    ACK('a', (p) -> "");                          // Acknowledgment packets without additional info

    private interface PacketLambda {
        String apply(Packet packet);
    }

    private final char tag;
    private final PacketLambda lambda;

    // Constructor to initialize the tag and lambda function for each packet type
    PacketTypes(char tag, PacketLambda lambda) {
        this.tag = tag;
        this.lambda = lambda;
    }

    public String getFileLine(Packet packet) {
        return tag + " " + lambda.apply(packet);
    }

    public char getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return tag + "";
    }
}
