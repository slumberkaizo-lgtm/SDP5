package delivery;


import java.util.List;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        List<Route> routes = DemoData.routes();
        List<RouteStrategy> strategies = List.of(
                new FastestRouteStrategy(), new CheapestRouteStrategy(),
                new EcoFriendlyRouteStrategy(), new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2),
                new DistanceFirstStrategy());
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD, 6000, 60, false);
        RoutePlanner planner = new RoutePlanner(strategies.get(0));

        System.out.println("RUNTIME SWITCHING: same planner, same request");
        print(planner.plan(routes, request), request);
        for (int i = 1; i < strategies.size(); i++) {
            planner.setStrategy(strategies.get(i));
            print(planner.plan(routes, request), request);
        }

        System.out.println("AUTOMATIC SELECTION: different requests");
        StrategySelector selector = new StrategySelector();
        List<DeliveryRequest> requests = List.of(
                new DeliveryRequest(DeliveryPriority.URGENT, 10000, 25, false),
                new DeliveryRequest(DeliveryPriority.ECONOMY, 2000, 60, false),
                new DeliveryRequest(DeliveryPriority.ECO, 3500, 45, true),
                new DeliveryRequest(DeliveryPriority.STANDARD, 6000, 60, false));
        for (DeliveryRequest next : requests) {
            planner.setStrategy(selector.select(next));
            System.out.println("Request: " + next);
            print(planner.plan(routes, next), next);
        }

        System.out.println("COMPARISON: same request, all registered strategies");
        StrategyComparison comparison = new StrategyComparison(strategies);
        for (RouteDecision decision : comparison.compare(routes, request)) {
            print(decision, request);
        }

        System.out.println("FAILURE SCENARIOS");
        try {
            planner.plan(List.of(), request);
        } catch (NoSuitableRouteException e) {
            System.out.println(e.getMessage());
        }
        try {
            planner.plan(routes, new DeliveryRequest(DeliveryPriority.STANDARD, 100, 5, false));
        } catch (NoSuitableRouteException e) {
            System.out.println(e.getMessage());
        }
        try {
            new BalancedRouteStrategy(0.5, 0.5, 0.5, 0.5);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void print(RouteDecision decision, DeliveryRequest request) {
        Route route = decision.route();
        System.out.printf("%s -> %s | time: %d min | cost: %.2f | distance: %.1f km | CO2: %.1f kg | score: %.4f%n",
                decision.strategyName(), route.id(), route.totalMinutes(), route.totalCost(request),
                route.distanceKm(), route.carbonEmission(), decision.score());
        System.out.println("Reason: " + decision.explanation());
        for (String rejection : decision.rejectedRoutes()) {
            System.out.println("Rejected: " + rejection);
        }
        System.out.println();
    }
}
