package assignment.Common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * The goal of this class is to have it act more like a struct than follow OOP principles, seeing as how I have getters
 * for all private member variables.
 */
public class ClientHandler {
    private String clientName;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    /*
     * It may be considered redundant to hold both the socket and the IOstreams. HOWEVER, if the objects are already
     * heap allocated destroying the objects makes no sense, and instead passing them it means I will keep the pointers alive.
     * Especially important as I am not implementing Runnable interface -> instead forking and creating a thread that runs handleClient();
     * Ultimately I am just keeping pointers to already opened IOStreams and Sockets so at most I lose 3 pointers of space.
     */
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