package assignment.Server;

import assignment.Client.Client;
import assignment.Common.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

// Server class
public class Server {
    private ServerSocket serverSocket;
    private int port;

    private static final String AUTHENTICATION_KEY = "SecretKey123";
    private static final String SALT = "RandomSalt";

    private static final int TEA_PREPARATION_TIME = 30000; // 30 seconds in milliseconds
    private static final int COFFEE_PREPARATION_TIME = 45000; // 45 seconds in milliseconds

    AtomicInteger numClients;
    private Map<Order, Client> orderToClientMap;
    private BlockingQueue<Order> waitingArea;
    private ExecutorService brewingArea;
    private BlockingQueue<Order> trayArea;

    public Server(int port) {
        this.port = port;
        this.orderToClientMap = new HashMap<>();
        this.waitingArea = new LinkedBlockingQueue<>();
        this.brewingArea = Executors.newFixedThreadPool(4); // 2 for tea, 2 for coffee
        this.trayArea = new LinkedBlockingQueue<>();
        this.numClients = new AtomicInteger(0);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                numClients.incrementAndGet();
                new Thread(
                        () -> handleClient(clientSocket)
                ).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeServer();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // Perform authentication
            if (!authenticate(reader, writer)) {
                System.out.println("Authentication failed. Closing connection.");
                return;
            }

            // Request Name
            writer.println("Please enter your name: ");
            String nameRq = reader.readLine();
            while (reader.readLine() != null || !Objects.equals(reader.readLine(), "\n")) {
                // Add further safety so the person doesn't enter some escape chars
                nameRq = reader.readLine().strip();
            }
            // Create ClientHandler for each seperate client.
            ClientHandler clientHandler = new ClientHandler(nameRq, clientSocket, reader, writer);

            // Process user requests
            String request;
            while ((request = reader.readLine()) != null) {
                System.out.println("Received from client: " + request);
                if (processRequest(request)){
                    numClients.decrementAndGet();
                    return;
                }
                String response = "Server response to: " + request;
                writer.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeClient(clientSocket);
            numClients.decrementAndGet(); // Decrease the client count after they leave.
        }
    }

    boolean processRequest(String request){
        if ("exit".equalsIgnoreCase(request)) {
            // Close the client connection gracefully
            return true;
        }
        if ("order")
        return false;
    }

    public static Order parseOrderString(String orderString) throws IllegalArgumentException {
        String[] tokens = orderString.split("\\s+");

        // token length must be at a minimum "order status" hence cannot be less than 2
        if (tokens.length < 2 || !tokens[0].equalsIgnoreCase("order")) {
            throw new IllegalArgumentException("Invalid order format");
        }

        int teaCount = 0;
        int coffeeCount = 0;

        for (int i = 1; i < tokens.length; i += 2) {
            int quantity;
            try {
                quantity = Integer.parseInt(tokens[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid quantity format");
            }

            if (i + 1 < tokens.length) {
                String drink = tokens[i + 1].toLowerCase();

                if (drink.contains("tea")) {
                    teaCount += quantity;
                } else if (drink.contains("coffee")) {
                    coffeeCount += quantity;
                } else {
                    throw new IllegalArgumentException("Invalid drink type");
                }
            } else {
                throw new IllegalArgumentException("Incomplete order details");
            }
        }

        return new Order("Customer", teaCount, coffeeCount);
    }

    private boolean authenticate(BufferedReader reader, PrintWriter writer) throws IOException {
        try {
            // Generate a nonce based on current date and time
            String nonce = generateNonce();

            writer.println(nonce);

            // Receive the hashed response from the client
            String clientHashedResponse = reader.readLine();

            // Calculate the expected hash based on the shared secret, salt, and nonce
            String secretWithSalt = AUTHENTICATION_KEY + SALT;
            String hashInput = secretWithSalt + nonce;
            String expectedHash = calculateHash(hashInput);

            // Compare the received hash with the expected hash
            return clientHashedResponse.equals(expectedHash);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String generateNonce() {
        LocalDateTime timestamp = LocalDateTime.now();
        return timestamp.toString();
    }

    private String calculateHash(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    private void closeServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server closed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeClient(Socket clientSocket) {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                System.out.println("Client connection closed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
