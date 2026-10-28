package delivery;


public record Location(String name, double latitude, double longitude) {
    public Location {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Location name is required");
        }
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }
    }
}
