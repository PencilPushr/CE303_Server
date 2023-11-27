package assignment;

import assignment.Client.Client;

public class Customer {
    public static void main(String[] args) {
        // Create a client and connect to the server
        Client client = new Client("localhost", 8080);
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> {
                    System.out.println("Shutting down gracefully...");
                    client.closeClient();
                })
        );
        client.connect();
    }
}
