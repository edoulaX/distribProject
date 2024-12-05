package cs451;

import java.net.*;

public class NetworkSimulator {
    public void send(Message message, String address, int port) throws Exception {
        byte[] data = message.toBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(address), port);
        DatagramSocket socket = new DatagramSocket();
        socket.send(packet);
        socket.close();
    }
}

