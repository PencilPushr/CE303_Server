package assignment;

import assignment.Client.*;
import assignment.Common.Order;
import assignment.Server.*;

import java.util.Arrays;
import java.util.Objects;

/*
 *
 * DO NOT EXECUTE THIS, THIS WAS FOR TESTING PURPOSES -> EXECUTE BARISTA AND CUSTOMER SEPARATELY!
 *
 */
public class Main {
    public static String parseOrderString(String orderString) throws IllegalArgumentException {
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

    private static String[] getTokens(String orderString) {
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


    public static void main(String[] args) {
        String orderString1 = "order 1 tea";
        String orderString2 = "order 1 coffee";
        String orderString3 = "order 1 tea and 1 coffee";
        String orderString4 = "order 2 teas and 3 coffees";
        String orderString5 = "order 1 coffee and 4 teas";
        String orderStatusString = "Order status for John:" +
                "\n- 1 coffee and 2 teas in waiting area" +
                "\n- 1 coffee and 1 tea currently being prepared" +
                "\n- 2 coffees currently in the tray";

        try {
            System.out.println(parseOrderString(orderString1));
            System.out.println(parseOrderString(orderString2));
            System.out.println(parseOrderString(orderString3));
            System.out.println(parseOrderString(orderString4));
            System.out.println(parseOrderString(orderString5));


        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }


        // Start the server in a separate thread
        //Server server = new Server(8080);
        //this is equivalent to "new Thread( () -> server.start() ).start();"
        //new Thread(server::start).start();

        // Create a client and connect to the server
        //Client client = new Client("localhost", 8080);
        //client.connect();

        /*
        // Create multiple clients to test concurrent functionality
        for (int i = 0; i < 5; i++) {
            Client client = new Client("localhost", 8080);
            new Thread(client::connect).start();
        }
         */
    }
}

