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
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

enum customerStatus {
    IDLE,
    WAITING,
    DEFAULT //this also doubles as an error
}

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
        // The main idea is that I just generate a separate thread for each tea and coffee, there's probably a smarter
        // way to do this, but I am not a smarter person.
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
                        //https://stackoverflow.com/questions/27524445/does-a-lambda-expression-create-an-object-on-the-heap-every-time-its-executed
                        () -> handleClient(clientSocket) // spawn new thread that calls this function, see stack overflow post above
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
                if (processRequest(request, reader, writer)){
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

    boolean processRequest(String request, BufferedReader reader, PrintWriter writer){
        String[] tokens = request.split("\\s+");

        // Can't do switch because can't use the .equals() func
        if ("exit".equalsIgnoreCase(request) || (tokens[0].equalsIgnoreCase("exit") && (tokens.length == 1))) {
            // Close the client connection by exiting the while loop and call closeClient();
            return true;
        }
        // Check this case first cuz otherwise it's gonna be a pain trying to add more conditions to parseOrderString();
        if ("order status".equalsIgnoreCase(request)) {
            returnOrderStatusToClient(writer);
            return false;
        }
        if (tokens[0].equalsIgnoreCase("order")) {
            parseOrderString(request);
            return false;
        }
        return false;
    }

    public void returnOrderStatusToClient(PrintWriter writer) {
        String orderStatus = new String();
        writer.println(orderStatus);
    }

    /*
     * Goal of the function is that if a client exits for whatever reason
     */
    void checkAndPassOrdersToOtherClients() {

    }

    public String parseOrderString(String orderString) throws IllegalArgumentException {
        String[] tokens = getTokens(orderString);
        System.out.println(Arrays.toString(Arrays.stream(tokens).toArray()));
        int teaCount = 0;
        int coffeeCount = 0;

        label:
        for (int i = 1; i < tokens.length; i+=3) {
            int quantity;
            try {
                quantity = Integer.parseInt(tokens[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid quantity format");
            }

            if (i + 1 >= tokens.length) {
                throw new IllegalArgumentException("Incomplete order details");
            }

            String drink = tokens[i + 1].toLowerCase();

            switch (drink) {
                case "tea":
                case "teas":
                    teaCount += quantity;
                    break label;
                case "coffee":
                case "coffees":
                    coffeeCount += quantity;
                    break label;
                case "and":
                    if (i + 2 >= tokens.length) {
                        throw new IllegalArgumentException("Incomplete order details after 'and'");
                    }
                    String nextDrink = tokens[i + 2].toLowerCase();
                    if (nextDrink.equals("tea") || nextDrink.equals("teas")) {
                        teaCount += quantity;
                    } else if (nextDrink.equals("coffee") || nextDrink.equals("coffees")) {
                        coffeeCount += quantity;
                    } else {
                        throw new IllegalArgumentException("Invalid drink type after 'and'");
                    }
                    i++; // Skip the next token as it has been processed

                    break label;
                default:
                    throw new IllegalArgumentException("Invalid drink type");
            }
        }

        return Arrays.toString(tokens);
    }

    private String[] getTokens(String orderString) {
        String[] tokens = orderString.split("\\s+");

        // At most the longest order that can currently exist is "order 2 teas and 2 coffees" -> 6 words long.
        // picked 32 cuz it can be processed in one cpu cycle
        if (tokens.length > 32) { throw new IllegalArgumentException("Order length too long"); }

        if (tokens.length < 3 || !tokens[0].equalsIgnoreCase("order")) {
            throw new IllegalArgumentException("Invalid order format");
        }

        // Can't have more than 1 and
        int andCounter = 0;
        for (String token : tokens) {
            if (Objects.equals(token, "and")) {
                andCounter++;
                if (andCounter > 1) throw new IllegalArgumentException("Invalid order format: Too many ands");
            }
        }

        return tokens;
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

        // Once the client has successfully exited
        // Attempt to pass on any orders
        checkAndPassOrdersToOtherClients();
    }
}
