package assignment;

import assignment.Server.Server;

public class Barista {
    public static void main(String[] args) {
        // Start the server in a separate thread
        Server server = new Server(8080); // Apparently it's recommended to also use the port 7979
        //this is equivalent to "new Thread( () -> server.start() ).start();"
        server.start();
        //new Thread(server::start).start();

    }
}
