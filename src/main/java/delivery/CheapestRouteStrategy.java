package delivery;


import java.util.Comparator;
import java.util.List;

public class CheapestRouteStrategy implements RouteStrategy {
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        var filtered = RouteConstraints.filter(routes, request);
        Route best = filtered.valid().stream()
                .min(Comparator.comparingDouble(route -> route.totalCost(request))).orElseThrow();
        double surcharge = request.priority() == DeliveryPriority.URGENT ? 500 : 0;
        return new RouteDecision(best, getStrategyName(), best.totalCost(request),
                "Lowest total cost: base " + best.baseCost() + " + distance "
                        + best.distanceKm() * 100 + " + priority " + surcharge
                        + " + fees " + best.additionalFees() + " = " + best.totalCost(request),
                filtered.rejected());
    }

    public String getStrategyName() { return "Cheapest"; }
}
