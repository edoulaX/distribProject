package cs451;

import cs451.parser.OutputParser;
import cs451.parser.PLConfigParser;
import cs451.parser.Parser;
import cs451.perfectLink.Process;
import cs451.perfectLink.Receiver;
import cs451.perfectLink.Sender;

import java.util.List;

public class Main {

    private static void handleSignal(Process process) {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        process.terminate();
        //write/flush output file if necessary
        System.out.println("Writing output.");
    }

    private static void initSignalHandlers(Process process) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal(process);
            }
        });
    }

    // Initializes the server as either a Sender or Receiver based on the host and destination configuration
    public static Process invokeProcess(Host host, Host dest, OutputParser output, PLConfigParser config) {
        return host.getId() == dest.getId()
                ? new Receiver(host, output, config)       // If the destination is the same as the host, it's a receiver
                : new Sender(host, dest, output, config);  // Otherwise, it's a sender
    }


    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();
        List<Host> hosts = parser.hosts();
        PLConfigParser configParser = new PLConfigParser(parser.config());

        int id = parser.myId(); //get the id of the current process
        Host host = hosts.get(id - 1); //get the current host by its id
        Host dest = hosts.get(configParser.getI() - 1); //get the destination host from the config
        Process process = invokeProcess(host, dest, parser.getOutputParser(), configParser);
        initSignalHandlers(process);
        process.run();
    }
}
