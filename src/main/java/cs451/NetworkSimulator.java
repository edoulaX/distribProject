package cs451;

import java.net.*;

public class NetworkSimulator {
    private static final int MAX_PACKET_SIZE = 8192; // Increased size to handle larger messages

    public void send(Message message, String address, int port) throws Exception {
        byte[] data = message.toBytes();
        if (data.length > MAX_PACKET_SIZE) {
            throw new IllegalArgumentException("Message size exceeds maximum packet size");
        }

        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(address), port);
        DatagramSocket socket = new DatagramSocket();
        socket.send(packet);
        socket.close();
    }
}
