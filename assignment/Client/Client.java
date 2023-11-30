package assignment.Client;

import assignment.Server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;

import static java.lang.System.exit;

public class Client {
    // Server stuff
    private String serverAddress;
    private int serverPort;

    // Authentication
    /*
     * Slight issue is that each key would have to be seperate and the server would essentially have to keep a track
     * of all the known keys which I haven't quite figured out how to do yet.
     * But the specs don't necessarily ask for that, and this is already hard enough as it is.
     */
    private static final String AUTHENTICATION_KEY = "SecretKey123";
    private static final String SALT = "RandomSalt";

    //IOstreams
    private BufferedReader serverReader;
    private PrintWriter clientWriter;
    private BufferedReader userInput;
    private ServerListener serverListener;
    private Thread SLThread;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.userInput = new BufferedReader(new InputStreamReader(System.in));
    }

    // Important note, as we are using a try-with-resources block, if anything fails, the resources section is freed!
    // This will lead to some exceptions being caught because they are null. This is not a bad thing, it just means
    // I need to add Object != null statements.
    public void connect() {
        try (Socket socket = new Socket(serverAddress, serverPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            this.serverReader = reader;
            this.clientWriter = writer;
            this.serverListener = new ServerListener(this.serverReader);

            // Attempt to authenticate with the server
            if (!authenticate()) {
                System.out.println("Authentication failed. Exiting.");
                return;
            }

            // If we successfully connected to the server continue to process the user commands
            System.out.println("Connected to the café. Type 'exit' to leave the café.");

            // Start the serverListener on a new thread after authentication
            SLThread = new Thread(serverListener);
            SLThread.start();

            // process user commands
            String input;
            while ( (input = userInput.readLine()) != null) {
                writer.println(input);

                if ("exit".equalsIgnoreCase(input)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeClient();
        }
    }

    private boolean authenticate() throws IOException {
        try {
            // Receive the nonce challenge from the server
            String nonce = serverReader.readLine();

            // Calculate the hash with the shared secret and salt
            String secretWithSalt = AUTHENTICATION_KEY + SALT;
            String hashInput = secretWithSalt + nonce;
            String hashedResponse = calculateHash(hashInput);

            // Send the hashed response back to the server
            clientWriter.println(hashedResponse);

            // The server will verify the response

            return true; // Authentication successful

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String calculateHash(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256"); // Set the encrypt algo.
        byte[] hash = digest.digest(data.getBytes()); // Generating the message digest as an array of bytes
        return Base64.getEncoder().encodeToString(hash);
    }

    // Slight issue, we actually can't ever write to the server if the clientWriter is closed due to the socket being
    // freed in the try-with-resources blocks.
    public void closeClient() {
        // Inform server we are leaving. Just going with the default case that we exit (this should remove the orders)
        clientWriter.println("exit");
        if (SLThread != null){
            SLThread.interrupt();
        }
        // Inform the user that we have closed.
        System.out.println("Client closed.");
    }
}
