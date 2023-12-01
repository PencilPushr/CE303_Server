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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Server class
public class Server {
    private ServerSocket serverSocket;
    private int port;

    private static final String AUTHENTICATION_KEY = "SecretKey123";
    private static final String SALT = "RandomSalt";

    AtomicInteger numClients;
    private static Brewery brewery;
    private static ArrayList<ClientHandler> clients;
    private OrderParser orderParser;

    public Server(int port) {
        this.port = port;
        clients = new ArrayList<>();
        brewery = new Brewery(this::printServerStatus);
        this.numClients = new AtomicInteger(0);
        this.orderParser = new OrderParser();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            new Thread(brewery).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                numClients.incrementAndGet();
                printServerStatus();
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
            writer.println("Authentication Successful");

            // Prints what ip, port and time of client connect
            clientConnectPrint(clientSocket);

            // Generate a new client handler to keep pointers to iostreams
            ClientHandler handler = requestNameAndGenerateCH(clientSocket, reader, writer);

            clients.add(handler);

            printServerStatus();

            // Process user requests ------------- Main Loop ---------------
            String request;
            while ( (request = reader.readLine()) != null) {
                System.out.println("Received from client: " + handler.getClientName() + " -> " + request);
                printServerStatus();
                if (processRequest(request, handler)){
                    printServerStatus();
                    clients.remove(handler);
                    return;
                }
            }
        } catch (IOException | IllegalArgumentException e ) {
            e.printStackTrace();
        } finally {
            closeClient(clientSocket);
            numClients.decrementAndGet(); // Decrease the client count after they leave.
            printServerStatus();
        }
    }

    void clientConnectPrint(Socket clientSocket) {
        System.out.println("Client connected: " + clientSocket.getInetAddress()
                + ":" + clientSocket.getLocalPort() + " conTime: " + System.currentTimeMillis());
    }

    void printServerStatus() {
        String timeStamp = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss").format(Calendar.getInstance().getTime());
        String str1 = "Current server status: " + timeStamp + "\n";
        String str2 = "Clients connected: " + numClients + "\n";
        String str3 = "Current customers waiting: " + brewery.getOrderQueueLength() + "\n";
        String str4 = "(Tea:Coffee) in waiting area: " + brewery.numTeasWaiting.get() + ":" + brewery.numCoffeesWaiting.get() + "\n" +
                    " brewing area: " + brewery.numTeasBrewing.get() + ":" + brewery.numCoffeesBrewing.get() + "\n" +
                    " tray area: " + brewery.numTeasTray.get() + ":" + brewery.numCoffeesTray.get() + "\n";
        System.out.println(str1 + str2 + str3 + str4);
    }

    ClientHandler requestNameAndGenerateCH(Socket clientSocket, BufferedReader stdin, PrintWriter stdout) throws IOException {
        // Request Name
        stdout.println("Please enter your name: ");

        String nameRq = null;
        while (nameRq == null || nameRq.trim().isEmpty() || nameRq.trim().equals("\n")) {
            // Read the input line
            nameRq = stdin.readLine();

            // Check if the input is null, empty, or contains only a newline character
            if (nameRq == null || nameRq.trim().isEmpty() || nameRq.trim().equals("\n")) {
                stdout.println("Invalid input. Please enter a non-empty name: ");
            }
        }
        stdout.println("Welcome " + nameRq + " please type menu to get options");
        // Create ClientHandler for each separate client.
        return new ClientHandler(nameRq.strip(), clientSocket, stdin, stdout, customerStatus.IDLE);
    }

    boolean processRequest(String request, ClientHandler clientHandler) throws IllegalArgumentException{

        String[] tokens = orderParser.getTokens(request);

        // Can't do switch because can't use the .equals() func
        if ("exit".equalsIgnoreCase(request) || (tokens[0].equalsIgnoreCase("exit") && (tokens.length == 1))) {
            // Close the client connection by exiting the while loop and call closeClient();
            return true;
        }
        if ("menu".equalsIgnoreCase(request) || (tokens[0].equalsIgnoreCase("menu") && (tokens.length == 1))) {
            printMenuToClient(clientHandler);
            return false;
        }
        // Check this case first cuz otherwise it's gonna be a pain trying to add more conditions to parseOrderString();
        if ("order status".equalsIgnoreCase(request) && (tokens.length == 2)) {
            returnOrderStatusToClient(clientHandler);
            return false;
        }
        if (tokens[0].equalsIgnoreCase("order")) {
            try {
                Order order = orderParser.parseOrderString(tokens, clientHandler.getClientName());
                brewery.addOrder(clientHandler, order);
            } catch (IllegalArgumentException e){
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

    private void printMenuToClient(ClientHandler clientHandler) {
        String str1 = "'exit' exits the current connection to the server\n" +
                "'menu' gives you menu of commands\n" +
                "'order status' gives you the current status on your drinks\n" +
                "you can issue any order via the following: order x tea(s)/coffee(s) [and y coffee(s)/tea(s)]";
        clientHandler.sendToClient(str1);
    }

    public void returnOrderStatusToClient(ClientHandler clientHandler) {
        String orderStatus = brewery.orderStatus(clientHandler);
        clientHandler.sendToClient(orderStatus);
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
