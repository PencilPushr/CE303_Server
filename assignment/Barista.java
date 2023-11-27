package assignment;

import assignment.Server.Server;

public class Barista {
    public static void main(String[] args) {
        // Start the server in a separate thread
        Server server = new Server(8080);
        //this is equivalent to "new Thread( () -> server.start() ).start();"
        server.start();
        //new Thread(server::start).start();

    }
}
