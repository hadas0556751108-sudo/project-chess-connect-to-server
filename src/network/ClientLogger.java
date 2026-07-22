package network;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClientLogger {
    private static final ClientLogger INSTANCE = new ClientLogger();
    private static PrintStream originalOut;
    private static PrintStream originalErr;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final List<String> buffer = new ArrayList<>();
    private PrintWriter writer;

    private ClientLogger() {}

    public static ClientLogger getInstance() {
        return INSTANCE;
    }

    public synchronized void setUsername(String username) {
        try {
            File dir = new File("logs");
            dir.mkdirs();
            if (writer != null) {
                writer.close();
            }
            File file = new File(dir, "client-" + username + ".log");
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8), true);
            for (String line : buffer) {
                writer.println(line);
            }
            buffer.clear();
        } catch (IOException e) {
            if (originalErr != null) {
                originalErr.println("Failed to open client log: " + e.getMessage());
            } else {
                e.printStackTrace();
            }
        }
    }

    public synchronized void log(String message) {
        String line = dateFormat.format(new Date()) + " " + message;
        if (originalOut != null) {
            originalOut.println(line);
        }
        if (writer != null) {
            writer.println(line);
        } else {
            buffer.add(line);
        }
    }

    public synchronized void logErr(String message) {
        String line = dateFormat.format(new Date()) + " [ERR] " + message;
        if (originalErr != null) {
            originalErr.println(line);
        }
        if (writer != null) {
            writer.println(line);
        } else {
            buffer.add(line);
        }
    }

    public void redirectSystemStreams() {
        originalOut = System.out;
        originalErr = System.err;
        try {
            System.setOut(new PrintStream(new LoggingOutputStream(this, false), true, StandardCharsets.UTF_8.name()));
            System.setErr(new PrintStream(new LoggingOutputStream(this, true), true, StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            if (originalErr != null) {
                originalErr.println("Failed to redirect system streams: " + e.getMessage());
            } else {
                e.printStackTrace();
            }
        }
    }

    private static class LoggingOutputStream extends OutputStream {
        private final ClientLogger logger;
        private final boolean error;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        LoggingOutputStream(ClientLogger logger, boolean error) {
            this.logger = logger;
            this.error = error;
        }

        @Override
        public synchronized void write(int b) {
            buffer.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            buffer.write(b, off, len);
        }

        @Override
        public synchronized void flush() {
            byte[] bytes = buffer.toByteArray();
            String text = new String(bytes, StandardCharsets.UTF_8);
            buffer.reset();
            int start = 0;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    String line = text.substring(start, i);
                    if (line.endsWith("\r")) {
                        line = line.substring(0, line.length() - 1);
                    }
                    if (error) {
                        logger.logErr(line);
                    } else {
                        logger.log(line);
                    }
                    start = i + 1;
                }
            }
            if (start < text.length()) {
                byte[] remaining = text.substring(start).getBytes(StandardCharsets.UTF_8);
                buffer.write(remaining, 0, remaining.length);
            }
        }
    }
}
