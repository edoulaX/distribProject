package cs451.perfectLink;

import cs451.Host;
import cs451.packet.Packet;
import cs451.packet.PacketTypes;
import cs451.parser.OutputParser;
import cs451.parser.PLConfigParser;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;


public class Sender extends Process {
    public static final int MAX_RETRANSMITION_TRIES = 5;
    
    private final int m;
    private final List<Integer> broadcastedList;

    private int seq_nr, retransmissionsTries;
    private boolean receiverAlive;
    private boolean waitingForReceiver = false;

    // Constructor to set up the sender with host and destination details and message count
    public Sender(Host host, Host dest, OutputParser outputParser, PLConfigParser config) {
        super(host, outputParser);
        this.m = config.getM();
        this.seq_nr = 1;   // Starts with sequence number 1
        this.retransmissionsTries = 0;
        this.receiverAlive = false;
        this.broadcastedList = new ArrayList<>();

        System.out.println("Binding " + host + "=> " + dest);
        try {
            this.socket.connect(dest.getSocketAddress());  // Connects socket to the destination address
            System.out.println(host + " Connected to " + dest);
        } catch (SocketException e) {
            terminate(e);
        }
    }

    @Override
    public void run() {
        running = true;
        System.out.println(host + "Running");
        while (running){
            if(seq_nr <= m){
                boolean broadcasted = broadcastAndAck();
                if (!broadcasted) {
                    running = prepareRetransmit(); // Prepare for retransmission if no ACK was received
                }
            } else {
                System.out.println(host + " Done transmitting");
                terminate();
            }

        }
    }

    private boolean broadcastAndAck() {
        Packet bc = broadcastPacket();  // Sends the broadcast packet
        if (bc == null)
            return false;

        if (!broadcastedList.contains(bc.getSeqNr())) {
            broadcastedList.add(bc.getSeqNr());
            outputParser.register(bc);  // Logs the broadcast event
        }

        return waitForAck();       // Waits for acknowledgment after broadcasting
    }

    // Broadcasts a packet with the current sequence number to the receiver
    private Packet broadcastPacket() {
        Packet packet = new Packet(PacketTypes.BROADCAST, seq_nr, host.getId());
        System.out.println(host + "Sending packet " + packet);
        try {
            sendPacket(packet);  // Sends packet through the socket
        } catch (PortUnreachableException | ClosedChannelException e) {
            System.out.println(host + " " + e.getMessage());
            return null;
        } catch (IOException e) {
            terminate(e);
        }
        return packet;
    }

    // Waits for an acknowledgment from the receiver
    private boolean waitForAck() {
        try { socket.setSoTimeout(timeout.get()); }  // Sets socket timeout for waiting on acknowledgment
        catch (SocketException e) { terminate(e); }

        DatagramPacket packet = getIncomingPacket();  // Attempts to receive acknowledgment packet

        if (packet == null) {
            System.out.println(host + "Receiver alive: " + receiverAlive + " Failed to receive ACK for seq_nr: " + seq_nr);
            if (receiverAlive)
                timeout.increase();  // Increase timeout if the receiver is unresponsive
            return false;
        }

        onAck(packet);  // Processes the received acknowledgment
        return true;
    }

    private void onAck(DatagramPacket packet) {
        receiverAlive = true;

        Packet ack = new Packet(PacketTypes.ACK, packet);
        System.out.println(host + "Received ACK: " + ack);
        String ackMsg = ack.getMsg();

        if (!delivered.contains(ackMsg) && ack.getSeqNr() == seq_nr) {
            delivered.add(ackMsg);  // Records the acknowledgment
            seq_nr++;                // Increments to the next sequence number
        }

        retransmissionsTries = 0;        // Resets retransmission counter
        timeout.decrease();         // Decreases the timeout for future sends
    }

    private boolean prepareRetransmit() {
        if( !receiverAlive ){
            if (!waitingForReceiver){
                System.out.println("Waiting for a receiver");
                waitingForReceiver = true;
            }
            return true;
        }
        ++retransmissionsTries;
        System.out.println(host + " Try Retransmition nb" + retransmissionsTries + " / " + MAX_RETRANSMITION_TRIES);
        return retransmissionsTries <= MAX_RETRANSMITION_TRIES;
    }

}
