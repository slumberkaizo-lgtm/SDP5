package delivery;


import java.util.Objects;

public record DeliveryRequest(DeliveryPriority priority, double maxBudget,
                              int maxDeliveryMinutes, boolean ecoPreferred) {
    public DeliveryRequest {
        Objects.requireNonNull(priority, "Priority is required");
        if (!Double.isFinite(maxBudget) || maxBudget < 0 || maxDeliveryMinutes <= 0) {
            throw new IllegalArgumentException("Budget must be non-negative and deadline positive");
        }
    }
}
