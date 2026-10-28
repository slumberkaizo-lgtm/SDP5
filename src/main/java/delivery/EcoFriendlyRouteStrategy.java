package delivery;


import java.util.Comparator;
import java.util.List;

public class EcoFriendlyRouteStrategy implements RouteStrategy {
    private double score(Route route) {
        return route.carbonEmission() * 0.9 + route.distanceKm() * 0.1;
    }

    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        var filtered = RouteConstraints.filter(routes, request);
        Route best = filtered.valid().stream()
                .min(Comparator.comparingDouble(this::score)).orElseThrow();
        return new RouteDecision(best, getStrategyName(), score(best),
                "Lowest eco score: " + best.carbonEmission() + " * 0.9 + "
                        + best.distanceKm() + " * 0.1 = " + score(best), filtered.rejected());
    }

    public String getStrategyName() { return "Eco friendly"; }
}
