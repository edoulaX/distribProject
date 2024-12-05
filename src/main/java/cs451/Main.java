package cs451;

import cs451.Process;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // Parse CLI arguments
        if (args.length < 5) {
            System.err.println("Usage: ./run.sh --id ID --hosts HOSTS --output OUTPUT CONFIG");
            return;
        }

        int id = Integer.parseInt(args[1]); // Process ID
        String hostsFile = args[3]; // Path to HOSTS file
        String outputFile = args[5]; // Path to output file
        String configFile = args[6]; // Path to CONFIG file

        // Load configuration
        List<Host> hosts = loadHosts(hostsFile);
        int totalProcesses = hosts.size();
        int m = loadConfig(configFile); // Number of messages to broadcast

        //System.out.println("Total processes: " + totalProcesses);
        //System.out.println("Number of messages: " + m);

        // Get process details
        Host myHost = hosts.get(id - 1); // IDs are 1-based

        // Initialize and start the process
        Process process = new Process(id, totalProcesses, myHost, hosts, outputFile, m);
        process.start();
    }

    private static List<Host> loadHosts(String hostsFile) throws IOException {
        List<Host> hosts = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(hostsFile));
        for (String line : lines) {
            String[] parts = line.split(" ");
            int id = Integer.parseInt(parts[0]);
            String host = parts[1];
            int port = Integer.parseInt(parts[2]);
            hosts.add(new Host(id, host, port));
        }
        return hosts;
    }

    private static int loadConfig(String configFile) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(configFile));
        return Integer.parseInt(lines.get(0).trim());
    }
}

