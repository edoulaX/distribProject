package cs451.parser;

import cs451.packet.Packet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class OutputParser {

    private static final String OUTPUT_KEY = "--output";

    private String path;
    private List<String> messages = new ArrayList<>();


    public boolean populate(String key, String value) {
        if (!key.equals(OUTPUT_KEY)) {
            return false;
        }

        File file = new File(value);
        path = file.getPath();
        onInit();
        return true;
    }


    /**
     * Clears the file content (if it exists).
     */
    private void onInit() {
        try (PrintWriter writer = new PrintWriter(path)) {
            writer.print("");  // Overwrite existing content with an empty string
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // Registers a packet's message in the message list to log it later
    public void register(Packet p) {
        messages.add(p.getFileLine());
    }

    /**
     * Writes all registered messages to the output file and clears the message list.
     */
    public void write() {
        System.out.println("WRITING TO FILE");  // Debug log
        System.out.println(messages);  // Prints messages for debugging
        System.out.println(path);
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(path, true))) {
            messages.forEach(pw::println);  // Write each message in the list to the file
        } catch (IOException e) {
            e.printStackTrace();
        }

        messages.clear();  // Clear the message list after writing
    }

    public String getPath() {
        return path;
    }

}
