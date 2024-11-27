package cs451.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PLConfigParser {
    private final int m, i;  // m: number of messages, i: number of processes

    public PLConfigParser(String path) {
        // Constructor reads the config file and initializes m and i
        try (InputStream stream = new FileInputStream(path)) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            String[] split = content.split(" ");
            m = Integer.parseInt(split[0].trim());
            i = Integer.parseInt(split[1].trim());
            System.out.println("PerfectLinkConfigParser - m: " + m + ", i: " + i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getM() {
        // Getter for number of messages
        return m;
    }

    public int getI() {
        // Getter for receiver index
        return i;
    }
}