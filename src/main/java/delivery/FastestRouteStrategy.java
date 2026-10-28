package delivery;


import java.util.Comparator;
import java.util.List;

public class FastestRouteStrategy implements RouteStrategy {
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        var filtered = RouteConstraints.filter(routes, request);
        Route best = filtered.valid().stream()
                .min(Comparator.comparingInt(Route::totalMinutes)).orElseThrow();
        return new RouteDecision(best, getStrategyName(), best.totalMinutes(),
                "Lowest final time: " + best.estimatedMinutes() + " + traffic "
                        + best.trafficPenaltyMinutes() + " = " + best.totalMinutes() + " min",
                filtered.rejected());
    }

    public String getStrategyName() { return "Fastest"; }
}
