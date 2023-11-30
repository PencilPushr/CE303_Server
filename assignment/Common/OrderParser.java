package assignment.Common;

import java.util.Objects;

public class OrderParser {

    public String[] getTokens(String orderString) {
        String[] tokens = orderString.split("\\s+");

        // At most the longest order that can currently exist is "order 2 teas and 2 coffees" -> 6 words long.
        // picked 32 cuz it can be processed in one cpu cycle
        if (tokens.length > 32) {
            throw new IllegalArgumentException("Order length too long");
        }

        // if token length is less than 3 break early because it could be "order status" or "exit" or "menu" (menu is not currently implemented)
        // or it could be garbage.
        if (tokens.length <= 2) {
            return tokens;
        }

        // Can't have more than 1 'and'
        int andCounter = 0;
        for (String token : tokens) {
            if (Objects.equals(token, "and")) {
                andCounter++;
                if (andCounter > 1) {
                    throw new IllegalArgumentException("Invalid order format: Too many ands");
                }
            }
        }

        return tokens;
    }

    public Order parseOrderString(String[] tokens, String clientName) throws IllegalArgumentException {
        int teaCount = 0;
        int coffeeCount = 0;

        // if somehow we received order 1 or anything less than 2 words, this is bad.
        if (tokens.length < 3 || !tokens[0].equalsIgnoreCase("order")) {
            throw new IllegalArgumentException("Invalid order format");
        }

        for (int i = 1; i < tokens.length; i += 3) {
            int quantity;
            try {
                quantity = Integer.parseInt(tokens[i]);
                if (quantity < 0) quantity = 0;
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
                    break;
                case "coffee":
                case "coffees":
                    coffeeCount += quantity;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid drink type");
            }
        }
        System.out.println(teaCount + " teas, " + coffeeCount + " coffees");
        return new Order(clientName, teaCount, coffeeCount);
    }
}