package delivery;


import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class BalancedRouteStrategy implements RouteStrategy {
    private final double timeWeight;
    private final double costWeight;
    private final double distanceWeight;
    private final double emissionWeight;

    public BalancedRouteStrategy(double timeWeight, double costWeight,
                                 double distanceWeight, double emissionWeight) {
        double[] weights = {timeWeight, costWeight, distanceWeight, emissionWeight};
        double sum = 0;
        for (double weight : weights) {
            if (!Double.isFinite(weight) || weight < 0 || weight > 1) {
                throw new IllegalArgumentException("Weights must be finite and between 0 and 1");
            }
            sum += weight;
        }
        if (Math.abs(sum - 1.0) > 0.000001) {
            throw new IllegalArgumentException("Weights must sum to 1");
        }
        this.timeWeight = timeWeight;
        this.costWeight = costWeight;
        this.distanceWeight = distanceWeight;
        this.emissionWeight = emissionWeight;
    }

    private double normalize(double value, List<Route> routes, ToDoubleFunction<Route> metric) {
        double min = routes.stream().mapToDouble(metric).min().orElseThrow();
        double max = routes.stream().mapToDouble(metric).max().orElseThrow();
        return max == min ? 0 : (value - min) / (max - min);
    }

    public double score(Route route, List<Route> validRoutes, DeliveryRequest request) {
        return normalize(route.totalMinutes(), validRoutes, Route::totalMinutes) * timeWeight
                + normalize(route.totalCost(request), validRoutes, r -> r.totalCost(request)) * costWeight
                + normalize(route.distanceKm(), validRoutes, Route::distanceKm) * distanceWeight
                + normalize(route.carbonEmission(), validRoutes, Route::carbonEmission) * emissionWeight;
    }

    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        var filtered = RouteConstraints.filter(routes, request);
        Route best = filtered.valid().stream().min(Comparator.comparingDouble(
                route -> score(route, filtered.valid(), request))).orElseThrow();
        double result = score(best, filtered.valid(), request);
        return new RouteDecision(best, getStrategyName(), result,
                "Lowest normalized weighted score = " + result + "; weights (time, cost, distance, CO2) = "
                        + timeWeight + ", " + costWeight + ", " + distanceWeight + ", " + emissionWeight,
                filtered.rejected());
    }

    public String getStrategyName() { return "Balanced"; }
}
