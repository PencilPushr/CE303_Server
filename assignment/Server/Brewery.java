package assignment.Server;

import assignment.Common.ClientHandler;
import assignment.Common.DrinkType;
import assignment.Common.Order;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class ClientOrder{
    int waitingAreaTeas = 0;
    int waitingAreaCoffees = 0;
    int trayAreaTeas = 0;
    int trayAreaCoffees = 0;
}

class HandleToOrder{
    ClientHandler handle;
    ClientOrder order = new ClientOrder();
}

public class Brewery implements Runnable {
    // As we only have 1 brewery in server, these all remain static.
    // In the event that we use multiple communicating servers, this will have to change
    AtomicInteger numTeasWaiting = new AtomicInteger(0);
    AtomicInteger numCoffeesWaiting = new AtomicInteger(0);

    AtomicInteger numTeasBrewing = new AtomicInteger(0);
    AtomicInteger numCoffeesBrewing = new AtomicInteger(0);

    AtomicInteger numTeasTray = new AtomicInteger(0);
    AtomicInteger numCoffeesTray = new AtomicInteger(0);
    final int maxBrewsPerDT = 2;

    HandleToOrder currentHandle = null;

    private static final int TEA_PREPARATION_TIME = 10000; // 30 seconds in milliseconds
    private static final int COFFEE_PREPARATION_TIME = 15000; // 45 seconds in milliseconds
    private static BlockingQueue<HandleToOrder> orderQueue;

    private Runnable statePrinter;

    ClientHandler[] brewingFor = { null, null, null, null };

    public Brewery(Runnable statePrinter) {
        orderQueue = new LinkedBlockingDeque<>();
        this.statePrinter = statePrinter;
    }

    public void addOrder(ClientHandler handle, Order order){
        handle.addOrder(order);
        numTeasWaiting.setPlain(numTeasWaiting.get() + order.getTeaCount());
        numCoffeesWaiting.setPlain(numCoffeesWaiting.get() + order.getCoffeeCount());
        for (var ho : orderQueue){
            if (ho.handle.getClientName().equals(handle.getClientName())){
                ho.order.waitingAreaTeas += order.getTeaCount();
                ho.order.waitingAreaCoffees += order.getCoffeeCount();
                numTeasWaiting.setPlain(numTeasWaiting.get() + order.getTeaCount());
                numCoffeesWaiting.setPlain(numCoffeesWaiting.get() + order.getCoffeeCount());

                // Send confirmation to client
                handle.sendToClient("[BREWER] -> order exists already " + handle.getClientName()
                        + " - updated to: Tea(" + handle.getTea() + ") Coffee(" + handle.getCoffee() + ")");
                statePrinter.run();
                return;
            }
        }
        HandleToOrder ho = new HandleToOrder();
        ho.handle = handle;
        ho.order.waitingAreaTeas = order.getTeaCount();
        ho.order.waitingAreaCoffees = order.getCoffeeCount();
        handle.sendToClient("[BREWER] -> order received for "+ handle.getClientName() + " (" + handle.getTea() + " tea(s) " +
            handle.getCoffee() + " and coffee(s))" );
        statePrinter.run();
        orderQueue.add(ho);
    }

    public synchronized void brewDrink(DrinkType drinkType, ClientHandler handle, ClientOrder order) {
        Thread startBrewing = new Thread(() -> {
            try {
                if (drinkType == DrinkType.TEA){
                    brewingFor[numTeasBrewing.get() - 1] = handle;

                    statePrinter.run();
                    Thread.sleep(TEA_PREPARATION_TIME);
                    statePrinter.run();

                    numTeasTray.incrementAndGet();
                    numTeasBrewing.decrementAndGet();
                    order.trayAreaTeas++;
                    order.waitingAreaTeas--;
                    brewingFor[numTeasBrewing.get()] = null;

                } else if (drinkType == DrinkType.COFFEE){
                    brewingFor[numCoffeesBrewing.get() - 1] = handle;

                    statePrinter.run();
                    Thread.sleep(COFFEE_PREPARATION_TIME);
                    statePrinter.run();

                    numCoffeesTray.incrementAndGet();
                    numCoffeesBrewing.decrementAndGet();
                    order.trayAreaCoffees++;
                    order.waitingAreaCoffees--;
                    brewingFor[numTeasBrewing.get()] = null;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        startBrewing.start();
    }

    synchronized void CompleteOrder(){
        if (numTeasTray.get() >= currentHandle.handle.getTea()
                && numCoffeesTray.get() >= currentHandle.handle.getCoffee()) {
            //Send message to client that order has been completed;
            currentHandle.handle.sendToClient("Finished brewing: " + currentHandle.handle.getTea()
                    + " Tea(s) " + currentHandle.handle.getCoffee() + " Coffee(s)" );
            orderQueue.poll();
            resetTrayCount();
            resetClientDrinkCount();
            currentHandle = null;
            statePrinter.run();
        }
    }

    void resetTrayCount() {
        numTeasTray.setPlain(numTeasTray.get() - currentHandle.handle.getTea());
        numCoffeesTray.setPlain(numCoffeesTray.get() - currentHandle.handle.getCoffee());
    }

    void resetClientDrinkCount() {
        currentHandle.handle.setTea(0);
        currentHandle.handle.setCoffee(0);
    }

    @Override
    public void run() {
        while (true) {
            if (currentHandle != null) {
                if (numTeasBrewing.get() < maxBrewsPerDT && numTeasWaiting.get() > 0) {
                    numTeasWaiting.decrementAndGet();
                    numTeasBrewing.incrementAndGet();
                    ClientOrder order = currentHandle.order;
                    //currentHandle.handle.getClientName();
                    brewDrink(DrinkType.TEA, currentHandle.handle, order);
                }
                if (numCoffeesBrewing.get() < maxBrewsPerDT && numCoffeesWaiting.get() > 0) {
                    numCoffeesWaiting.decrementAndGet();
                    numCoffeesBrewing.incrementAndGet();
                    ClientOrder order = currentHandle.order;
                    brewDrink(DrinkType.COFFEE, currentHandle.handle, order);
                }
                CompleteOrder();
            } else {
                currentHandle = orderQueue.peek();
            }

        }
    }

    public String orderStatus(ClientHandler clientHandler) {
        for (var i : orderQueue) {
            if (i.handle.equals(clientHandler)) {

                i.order.
            }
        }
    }

    public int getOrderQueueLength() {
        return orderQueue.size();
    }

    /*
    public boolean processOrder(Order order, ClientIO clientIO) {
        orderToClientMap.putIfAbsent(order, clientIO);
        statustoClientMap.putIfAbsent();
    }
    */

}
