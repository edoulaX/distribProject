package cs451;

import java.io.IOException;
import java.net.*;

public class NetworkSimulator {
    private static final int MAX_PACKET_SIZE = 8192; // Maximum size for UDP packets
    private final DatagramSocket socket;

    public NetworkSimulator() throws SocketException {
        this.socket = new DatagramSocket(); // Single socket instance for sending
    }

    public void send(Message message, String address, int port) throws IOException {
        byte[] data = message.toBytes();

        // Check if the message size exceeds the maximum allowed
        if (data.length > MAX_PACKET_SIZE) {
            throw new IllegalArgumentException("Message size exceeds maximum packet size of " + MAX_PACKET_SIZE + " bytes");
        }

        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(address), port);
        socket.send(packet);
    }

    public void close() {
        socket.close(); // Ensure socket is closed properly when done
    }
}
