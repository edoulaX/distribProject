package cs451.perfectLink;

import cs451.Host;
import cs451.packet.Packet;
import cs451.packet.PacketTypes;
import cs451.parser.OutputParser;
import cs451.parser.PLConfigParser;

import java.io.IOException;
import java.net.DatagramPacket;

public class Receiver extends Process{

    private final int nbMessages;
    private final int nbProcesses;

    public Receiver(Host host, OutputParser outputParser, PLConfigParser config) {
        super(host, outputParser);
        this.nbMessages = config.getM();
        this.nbProcesses = config.getI();
    }

    // Sends an acknowledgment (ACK) packet back to the sender in response to a received broadcast
    protected void sendAck(DatagramPacket dg, Packet broadcast) {
        Packet ack = new Packet(PacketTypes.ACK, broadcast.getSeqNr(), host.getId());  // Creates ACK packet
        System.out.println(host + "Sending ACK to: " + dg.getPort() + ", msg: " + ack.getMsg());
        try {
            sendPacket(ack, dg);  // Sends the ACK packet
        } catch (IOException e) {
            terminate(e);  // Terminates if there is an error in sending the ACK
        }
    }

    @Override
    public void run() {
        running = true;
        System.out.println(host + "Running");
        while (running){
            System.out.println(host + "receiver waiting...");  // Logs that the receiver is waiting for incoming packets

            DatagramPacket packet = getIncomingPacket();  // Receives an incoming packet
            Packet bc = new Packet(PacketTypes.DELIVER, packet);  // Wraps received packet as a DELIVER type

            System.out.println(host + "Received from: " + packet.getPort() + ", msg: " + bc.getMsg());  // Debug log

            sendAck(packet, bc);  // Sends an acknowledgment back to the sender

            // Checks if the message has already been delivered, to prevent duplicates
            if (!delivered.contains(bc.getMsg())) {
                outputParser.register(bc);           // Registers the received message for logging
                delivered.add(bc.getMsg());     // Adds message to delivered list to prevent re-delivery
            }

            if(delivered.size() == nbMessages * (nbProcesses-1) ){
                System.out.println(host + " Receiver Done Delivering");
                terminate();
            }
        }
    }

}
