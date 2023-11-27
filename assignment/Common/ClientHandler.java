package assignment.Common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler {
    private String clientName;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public ClientHandler(String clientName, Socket socket, BufferedReader reader, PrintWriter writer) {
        this.clientName = clientName;
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
    }

    public String getClientName() {
        return clientName;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public Socket getSocket() {
        return socket;
    }

    public void sendToClient(String message) {
        writer.println(message);
    }

    public String receiveFromClient() throws IOException {
        return reader.readLine();
    }
}