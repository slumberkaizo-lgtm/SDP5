package delivery;


import java.util.List;

public final class DemoData {
    private DemoData() {}

    public static List<Route> routes() {
        Location origin = new Location("Warehouse", 43.238, 76.945);
        Location destination = new Location("Customer", 43.256, 76.928);
        return List.of(
                new Route("R1 Express", origin, destination, 18, 20, 3000, 4.5, 8, 500, true),
                new Route("R2 Highway", origin, destination, 24, 18, 2000, 5.0, 2, 700, true),
                new Route("R3 Economy", origin, destination, 12, 45, 500, 2.8, 5, 0, true),
                new Route("R4 Electric", origin, destination, 14, 35, 1200, 0.5, 3, 100, true),
                new Route("R5 Short", origin, destination, 8, 30, 1400, 2.0, 12, 100, true),
                new Route("R6 Balanced", origin, destination, 15, 25, 1100, 1.8, 4, 100, true),
                new Route("R7 Closed", origin, destination, 5, 10, 100, 0.1, 0, 0, false),
                new Route("R8 Premium", origin, destination, 20, 15, 6000, 6.0, 1, 1000, true));
    }
}
