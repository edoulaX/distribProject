package cs451.perfectLink;

import cs451.Host;
import cs451.packet.Packet;
import cs451.parser.OutputParser;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;

abstract public class Process {
    private final byte[] data = new byte[32];

    protected final DatagramSocket socket; // Socket for sending and receiving DatagramPackets
    protected final OutputParser outputParser; // Handles file output
    protected final Host host;

    protected final List<String> delivered; // Tracks delivered messages to prevent duplicates
    protected final Timeout timeout; // Timeout management for retransmissions

    protected boolean running;

    public Process(Host host, OutputParser outputParser) {
        this.host = host;
        this.delivered = new ArrayList<>();
        this.outputParser = outputParser;
        this.timeout = new Timeout();
        this.running = false;

        String ip = host.getIp();
        int port = host.getPort();

        try {
            this.socket = new DatagramSocket(host.getSocketAddress());
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        System.out.println(host + "Socket opened");
    }

    // TODO Changes here
    protected DatagramPacket getIncomingPacket() {
        long a = System.nanoTime();
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length);
            socket.receive(packet);
            return packet;
        } catch (SocketTimeoutException e) {
            long b = System.nanoTime();
            long delta = (b - a) / 1000000;
            System.out.println(host + "INCOMING PACKET TIMEOUT: " + timeout.get() + "ms, delta: " + delta + "ms");
        } catch (PortUnreachableException | ClosedChannelException ignored) {}
        catch (IOException e) {
            terminate(e);
        }
        return null;
    }

    protected void sendPacket(Packet packet, DatagramPacket dg) throws IOException {
        DatagramPacket datagram = packet.getDatagram();
        datagram.setAddress(dg.getAddress());
        datagram.setPort(dg.getPort());
        socket.send(datagram);      // Sends the datagram packet
    }

    // TODO Changes here what does DatagramFunc
    protected void sendPacket(Packet packet) throws IOException {
        DatagramPacket datagramPacket = packet.getDatagram();
        socket.send(datagramPacket);
    }

    abstract public void run();


    protected void terminate(Exception e) {
        e.printStackTrace();
        terminate();
    }

    public void terminate() {
        if (running) {
            running = false;
            socket.close();
            outputParser.write();
            System.out.println(host + "Terminated");
        }
    }
}
