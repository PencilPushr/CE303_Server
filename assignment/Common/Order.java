package assignment.Common;

public class Order {
    private String clientName;
    private int teaCount;
    private int coffeeCount;

    public Order(String clientName, int teaCount, int coffeeCount) {
        this.clientName = clientName;
        this.teaCount = teaCount;
        this.coffeeCount = coffeeCount;
    }

    public String getClientName() {
        return clientName;
    }

    public int getTeaCount() {
        return teaCount;
    }

    public int getCoffeeCount() {
        return coffeeCount;
    }
}
