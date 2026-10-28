package delivery;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RoutePlannerTest {
    private final List<Route> routes = DemoData.routes();
    private final DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD, 6000, 60, false);

    private Route route(String id, double distance, int minutes, double base, double co2,
                        int traffic, double fees, boolean available) {
        return new Route(id, routes.get(0).origin(), routes.get(0).destination(),
                distance, minutes, base, co2, traffic, fees, available);
    }

    private List<RouteStrategy> strategies() {
        return List.of(new FastestRouteStrategy(), new CheapestRouteStrategy(),
                new EcoFriendlyRouteStrategy(), new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2),
                new DistanceFirstStrategy());
    }

    @Test
    void fastestChoosesLowestFinalTime() {
        assertEquals("R2 Highway", new FastestRouteStrategy().selectRoute(routes, request).route().id());
    }

    @Test
    void trafficCanChangeFastestWinner() {
        Route a = route("A", 10, 10, 100, 1, 20, 0, true);
        Route b = route("B", 10, 20, 100, 1, 0, 0, true);
        assertEquals(b, new FastestRouteStrategy().selectRoute(List.of(a, b), request).route());
    }

    @Test
    void cheapestChoosesLowestTotalCost() {
        assertEquals("R3 Economy", new CheapestRouteStrategy().selectRoute(routes, request).route().id());
    }

    @Test
    void cheapestUsesDistanceAndFeesRatherThanBaseAlone() {
        Route a = route("A", 20, 20, 100, 1, 0, 500, true);
        Route b = route("B", 5, 20, 500, 1, 0, 0, true);
        assertEquals(b, new CheapestRouteStrategy().selectRoute(List.of(a, b), request).route());
    }

    @Test
    void urgentSurchargeIsIncludedInCost() {
        Route route = route("A", 10, 20, 1000, 1, 0, 200, true);
        DeliveryRequest urgent = new DeliveryRequest(DeliveryPriority.URGENT, 10000, 60, false);
        assertEquals(2700, route.totalCost(urgent), 0.001);
        assertEquals(2200, route.totalCost(request), 0.001);
    }

    @Test
    void ecoChoosesLowestEnvironmentalScore() {
        RouteDecision decision = new EcoFriendlyRouteStrategy().selectRoute(routes, request);
        assertEquals("R4 Electric", decision.route().id());
        assertEquals(1.85, decision.score(), 0.000001);
    }

    @Test
    void ecoConsidersDistanceAsWellAsEmissions() {
        Route a = route("A", 40, 20, 100, 0.1, 0, 0, true);
        Route b = route("B", 5, 20, 100, 1, 0, 0, true);
        assertEquals(b, new EcoFriendlyRouteStrategy().selectRoute(List.of(a, b), request).route());
    }

    @Test
    void balancedCombinesNormalizedCriteria() {
        RouteDecision decision = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2).selectRoute(routes, request);
        assertEquals("R6 Balanced", decision.route().id());
        assertEquals(0.3048611111, decision.score(), 0.000001);
    }

    @Test
    void balancedWeightsCanChangeWinner() {
        assertEquals("R2 Highway", new BalancedRouteStrategy(1, 0, 0, 0).selectRoute(routes, request).route().id());
        assertEquals("R3 Economy", new BalancedRouteStrategy(0, 1, 0, 0).selectRoute(routes, request).route().id());
    }

    @Test
    void balancedHandlesEqualValuesWithoutDivisionByZero() {
        Route a = route("A", 10, 20, 100, 1, 0, 0, true);
        Route b = route("B", 10, 20, 100, 1, 0, 0, true);
        RouteDecision decision = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2).selectRoute(List.of(a, b), request);
        assertEquals(a, decision.route());
        assertEquals(0, decision.score());
    }

    @Test
    void balancedRejectsInvalidWeightSum() {
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(0.5, 0.5, 0.5, 0.5));
    }

    @Test
    void balancedRejectsNegativeAndNonFiniteWeights() {
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(-0.1, 0.6, 0.3, 0.2));
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(Double.NaN, 0.3, 0.1, 0.2));
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(Double.POSITIVE_INFINITY, 0, 0, 0));
    }

    @Test
    void samePlannerSupportsThreeRuntimeSwitches() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        assertEquals("R2 Highway", planner.plan(routes, request).route().id());
        planner.setStrategy(new CheapestRouteStrategy());
        assertEquals("R3 Economy", planner.plan(routes, request).route().id());
        planner.setStrategy(new EcoFriendlyRouteStrategy());
        assertEquals("R4 Electric", planner.plan(routes, request).route().id());
        planner.setStrategy(new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2));
        assertEquals("R6 Balanced", planner.plan(routes, request).route().id());
    }

    @Test
    void selectorMapsEveryPriority() {
        StrategySelector selector = new StrategySelector();
        assertInstanceOf(FastestRouteStrategy.class, selector.select(new DeliveryRequest(DeliveryPriority.URGENT, 6000, 60, false)));
        assertInstanceOf(CheapestRouteStrategy.class, selector.select(new DeliveryRequest(DeliveryPriority.ECONOMY, 6000, 60, false)));
        assertInstanceOf(EcoFriendlyRouteStrategy.class, selector.select(new DeliveryRequest(DeliveryPriority.ECO, 6000, 60, false)));
        assertInstanceOf(BalancedRouteStrategy.class, selector.select(request));
    }

    @Test
    void ecoPreferenceOverridesStandardButNotUrgent() {
        StrategySelector selector = new StrategySelector();
        assertInstanceOf(EcoFriendlyRouteStrategy.class, selector.select(new DeliveryRequest(DeliveryPriority.STANDARD, 6000, 60, true)));
        assertInstanceOf(FastestRouteStrategy.class, selector.select(new DeliveryRequest(DeliveryPriority.URGENT, 6000, 60, true)));
    }

    @Test
    void selectorAllowsReplacingMapping() {
        StrategySelector selector = new StrategySelector();
        selector.register(DeliveryPriority.STANDARD, new DistanceFirstStrategy());
        assertEquals("R5 Short", selector.select(request).selectRoute(routes, request).route().id());
    }

    @Test
    void everyStrategyRejectsUnavailableRoutes() {
        Route closed = route("Closed", 0, 1, 0, 0, 0, 0, false);
        Route valid = route("Valid", 10, 20, 100, 1, 0, 0, true);
        for (RouteStrategy strategy : strategies()) {
            assertEquals(valid, strategy.selectRoute(List.of(closed, valid), request).route());
        }
    }

    @Test
    void everyStrategyRespectsBudget() {
        DeliveryRequest limited = new DeliveryRequest(DeliveryPriority.STANDARD, 2000, 60, false);
        for (RouteStrategy strategy : strategies()) {
            assertEquals("R3 Economy", strategy.selectRoute(routes, limited).route().id());
        }
    }

    @Test
    void everyStrategyRespectsDeadlineIncludingTraffic() {
        Route late = route("Late", 1, 15, 0, 0, 10, 0, true);
        Route onTime = route("On time", 10, 20, 100, 1, 0, 0, true);
        DeliveryRequest limited = new DeliveryRequest(DeliveryPriority.STANDARD, 6000, 20, false);
        for (RouteStrategy strategy : strategies()) {
            assertEquals(onTime, strategy.selectRoute(List.of(late, onTime), limited).route());
        }
    }

    @Test
    void exactBudgetAndDeadlineAreAllowed() {
        Route exact = route("Exact", 10, 20, 1000, 1, 5, 200, true);
        DeliveryRequest limited = new DeliveryRequest(DeliveryPriority.STANDARD, 2200, 25, false);
        for (RouteStrategy strategy : strategies()) {
            assertEquals(exact, strategy.selectRoute(List.of(exact), limited).route());
        }
    }

    @Test
    void urgentSurchargeCanMakeRouteUnaffordable() {
        Route route = route("A", 10, 20, 1000, 1, 0, 0, true);
        DeliveryRequest urgent = new DeliveryRequest(DeliveryPriority.URGENT, 2000, 60, false);
        assertThrows(NoSuitableRouteException.class, () -> new CheapestRouteStrategy().selectRoute(List.of(route), urgent));
    }

    @Test
    void noSuitableRouteHasExplicitFailure() {
        DeliveryRequest impossible = new DeliveryRequest(DeliveryPriority.STANDARD, 100, 5, false);
        for (RouteStrategy strategy : strategies()) {
            NoSuitableRouteException error = assertThrows(NoSuitableRouteException.class,
                    () -> strategy.selectRoute(routes, impossible));
            assertTrue(error.getMessage().contains("budget exceeded"));
            assertTrue(error.getMessage().contains("deadline exceeded"));
        }
    }

    @Test
    void emptyRoutesHaveExplicitFailure() {
        for (RouteStrategy strategy : strategies()) {
            assertThrows(NoSuitableRouteException.class, () -> strategy.selectRoute(List.of(), request));
        }
    }

    @Test
    void fifthStrategyChoosesShortestAvailableRoute() {
        RouteDecision decision = new DistanceFirstStrategy().selectRoute(routes, request);
        assertEquals("R5 Short", decision.route().id());
        assertEquals(8, decision.score());
    }

    @Test
    void comparisonExecutesAllStrategiesForSameRequest() {
        List<RouteDecision> decisions = new StrategyComparison(strategies()).compare(routes, request);
        assertEquals(List.of("R2 Highway", "R3 Economy", "R4 Electric", "R6 Balanced", "R5 Short"),
                decisions.stream().map(d -> d.route().id()).toList());
    }

    @Test
    void sameStrategyCanChooseDifferentRouteForDifferentRequest() {
        FastestRouteStrategy strategy = new FastestRouteStrategy();
        DeliveryRequest generous = new DeliveryRequest(DeliveryPriority.STANDARD, 10000, 60, false);
        assertEquals("R2 Highway", strategy.selectRoute(routes, request).route().id());
        assertEquals("R8 Premium", strategy.selectRoute(routes, generous).route().id());
    }

    @Test
    void decisionExplainsChoiceAndRejections() {
        for (RouteStrategy strategy : strategies()) {
            RouteDecision decision = strategy.selectRoute(routes, request);
            assertFalse(decision.explanation().isBlank());
            assertTrue(decision.rejectedRoutes().contains("R7 Closed: unavailable"));
            assertTrue(decision.rejectedRoutes().contains("R8 Premium: budget exceeded"));
        }
    }

    @Test
    void routeRejectsNegativeOrNonFiniteData() {
        assertThrows(IllegalArgumentException.class, () -> route("A", -1, 20, 100, 1, 0, 0, true));
        assertThrows(IllegalArgumentException.class, () -> route("A", 10, 20, -100, 1, 0, 0, true));
        assertThrows(IllegalArgumentException.class, () -> route("A", Double.NaN, 20, 100, 1, 0, 0, true));
    }

    @Test
    void missingDataIsRejected() {
        assertThrows(NullPointerException.class, () -> new DeliveryRequest(null, 1000, 60, false));
        assertThrows(NullPointerException.class, () -> new RoutePlanner(null));
        assertThrows(NullPointerException.class, () -> new FastestRouteStrategy().selectRoute(routes, null));
        assertThrows(NullPointerException.class, () -> new FastestRouteStrategy().selectRoute(null, request));
    }

    @Test
    void invalidRequestIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DeliveryRequest(DeliveryPriority.STANDARD, -1, 60, false));
        assertThrows(IllegalArgumentException.class, () -> new DeliveryRequest(DeliveryPriority.STANDARD, 1000, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new DeliveryRequest(DeliveryPriority.STANDARD, Double.NaN, 60, false));
    }

    @Test
    void zeroBudgetAllowsFreeRoute() {
        Route free = route("Free", 0, 10, 0, 0, 0, 0, true);
        DeliveryRequest zero = new DeliveryRequest(DeliveryPriority.STANDARD, 0, 10, false);
        assertEquals(free, new CheapestRouteStrategy().selectRoute(List.of(free), zero).route());
    }
}
