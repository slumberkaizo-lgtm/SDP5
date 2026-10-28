package delivery;


import java.util.Comparator;
import java.util.List;

public class DistanceFirstStrategy implements RouteStrategy {
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        var filtered = RouteConstraints.filter(routes, request);
        Route best = filtered.valid().stream()
                .min(Comparator.comparingDouble(Route::distanceKm)).orElseThrow();
        return new RouteDecision(best, getStrategyName(), best.distanceKm(),
                "Lowest distance: " + best.distanceKm() + " km", filtered.rejected());
    }

    public String getStrategyName() { return "Distance first"; }
}
