package delivery;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RouteConstraints {
    private RouteConstraints() {}

    public record FilteredRoutes(List<Route> valid, List<String> rejected) {}

    public static FilteredRoutes filter(List<Route> routes, DeliveryRequest request) {
        Objects.requireNonNull(routes, "Routes are required");
        Objects.requireNonNull(request, "Request is required");
        List<Route> valid = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (Route route : routes) {
            Objects.requireNonNull(route, "Route is required");
            List<String> reasons = new ArrayList<>();
            if (!route.available()) reasons.add("unavailable");
            if (route.totalCost(request) > request.maxBudget()) reasons.add("budget exceeded");
            if (route.totalMinutes() > request.maxDeliveryMinutes()) reasons.add("deadline exceeded");
            if (reasons.isEmpty()) {
                valid.add(route);
            } else {
                rejected.add(route.id() + ": " + String.join(", ", reasons));
            }
        }
        if (valid.isEmpty()) {
            throw new NoSuitableRouteException("No suitable route. "
                    + (routes.isEmpty() ? "Route list is empty." : String.join("; ", rejected)));
        }
        return new FilteredRoutes(List.copyOf(valid), List.copyOf(rejected));
    }
}
