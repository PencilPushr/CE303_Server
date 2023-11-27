package assignment;

import assignment.Client.*;
import assignment.Server.*;

public class Main {
    public static void main(String[] args) {
        // Start the server in a separate thread
        Server server = new Server(8080);
        //this is equivalent to "new Thread( () -> server.start() ).start();"
        new Thread(server::start).start();

        // Create a client and connect to the server
        Client client = new Client("localhost", 8080);
        client.connect();

        /*
        // Create multiple clients to test concurrent functionality
        for (int i = 0; i < 5; i++) {
            Client client = new Client("localhost", 8080);
            new Thread(client::connect).start();
        }
         */
    }
}
