package delivery;


import java.util.Objects;

public record Route(String id, Location origin, Location destination,
                    double distanceKm, int estimatedMinutes, double baseCost,
                    double carbonEmission, int trafficPenaltyMinutes,
                    double additionalFees, boolean available) {
    public Route {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Route ID is required");
        }
        Objects.requireNonNull(origin, "Origin is required");
        Objects.requireNonNull(destination, "Destination is required");
        if (!Double.isFinite(distanceKm) || distanceKm < 0
                || !Double.isFinite(baseCost) || baseCost < 0
                || !Double.isFinite(carbonEmission) || carbonEmission < 0
                || !Double.isFinite(additionalFees) || additionalFees < 0
                || estimatedMinutes <= 0 || trafficPenaltyMinutes < 0
                || (long) estimatedMinutes + trafficPenaltyMinutes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid route values");
        }
    }

    public int totalMinutes() {
        return estimatedMinutes + trafficPenaltyMinutes;
    }

    public double totalCost(DeliveryRequest request) {
        double prioritySurcharge = request.priority() == DeliveryPriority.URGENT ? 500 : 0;
        return baseCost + distanceKm * 100 + prioritySurcharge + additionalFees;
    }
}
