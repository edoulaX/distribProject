package cs451;

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
        List<Set<Integer>> proposals = loadConfig(configFile);

        // Get process details
        Host myHost = hosts.get(id - 1); // IDs are 1-based

        // Initialize and start the process
        Process process = new Process(id, totalProcesses, myHost, hosts, outputFile, proposals);
        process.start();
    }

    private static List<Host> loadHosts(String hostsFile) throws IOException {
        List<Host> hosts = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(hostsFile));
        for (String line : lines) {
            String[] parts = line.split(" ");
            int id = Integer.parseInt(parts[0]);
            String address = parts[1];
            int port = Integer.parseInt(parts[2]);
            hosts.add(new Host(id, address, port));
        }
        return hosts;
    }

    private static List<Set<Integer>> loadConfig(String configFile) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(configFile));
        String[] header = lines.get(0).split(" ");
        int p = Integer.parseInt(header[0]); // Number of proposals per process

        List<Set<Integer>> proposals = new ArrayList<>();
        for (int i = 1; i <= p; i++) {
            String[] elements = lines.get(i).split(" ");
            Set<Integer> proposal = new HashSet<>();
            for (String element : elements) {
                proposal.add(Integer.parseInt(element));
            }
            proposals.add(proposal);
        }

        return proposals;
    }
}
