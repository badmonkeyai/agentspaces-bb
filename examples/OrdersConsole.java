// Copyright 2026 Bad Monkey, Inc.
// Licensed under the Apache License, Version 2.0.
import ai.badmonkey.agentspaces.api.space.Lease;
import ai.badmonkey.agentspaces.api.space.Template;
import ai.badmonkey.agentspaces.common.id.AgentId;
import ai.badmonkey.agentspaces.console.ConsolePanel;
import ai.badmonkey.agentspaces.console.ConsoleView;
import ai.badmonkey.agentspaces.console.FleetConsoleServer;
import ai.badmonkey.agentspaces.identity.PeerIdentity;
import ai.badmonkey.agentspaces.space.local.LocalSpace;
import java.time.Duration;

public class OrdersConsole {
    public record Order(String orderId, String item) {}

    public static void main(String[] args) throws Exception {
        var agent = new AgentId(PeerIdentity.generate().peerId(), "demo");
        var orders = LocalSpace.builder("orders", agent).build();
        orders.write(new Order("o-1", "kite"), Lease.of(Duration.ofHours(1)));
        var view = ConsoleView.builder().space("orders", orders, Order.class).build();

        // Register this panel on an existing host to expose its own space.
        var panel = new ConsolePanel() {
            public String id() { return "order-entries"; }
            public String title() { return "Orders"; }
            public Object data() { return orders.readAll(Template.of(Order.class), 100); }
        };
        try (var console = FleetConsoleServer.builder(view).panel(panel).build()) {
            int port = console.start(args.length == 0 ? 8080 : Integer.parseInt(args[0]));
            System.out.println("Listening on port " + port);
            Thread.currentThread().join();
        }
    }
}
