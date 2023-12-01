package assignment.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;

import static java.lang.System.exit;


public class ServerListener implements Runnable {
    final BufferedReader bufferedReader;

    ServerListener(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
    }

    @Override
    public void run() {
        try {
            // This will always throw an exception
            while (true) {
                String msg = bufferedReader.readLine();
                if (msg != null) {
                    System.out.println("[SERVER]: ");
                    System.out.println(msg);
                } else {
                    // End of stream, break out of the loop
                    break;
                }
            }
        } catch (IOException e) {
            // Handle the exception or log it
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) bufferedReader.close();
            } catch (IOException e) {
                // Handle the exception or log it
                e.printStackTrace();
            }
        }
    }
}

/*
public class ServerListener implements Runnable {
    final BufferedReader bufferedReader;

    ServerListener(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String msg = bufferedReader.readLine();
                if (msg != null) {
                    System.out.println("[SERVER]: ");
                    System.out.println(msg);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
*/