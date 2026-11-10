// Copyright 2026 Bad Monkey, Inc.
// Licensed under the Apache License, Version 2.0.
package agentspaces.bb;

import ai.badmonkey.agentspaces.api.ad.GroupAdvertisement;
import ai.badmonkey.agentspaces.api.ad.PeerAdvertisement;
import ai.badmonkey.agentspaces.api.space.ConflictStrategyType;
import ai.badmonkey.agentspaces.api.space.Lease;
import ai.badmonkey.agentspaces.api.space.Template;
import ai.badmonkey.agentspaces.common.codec.CborCodec;
import ai.badmonkey.agentspaces.common.id.GroupId;
import ai.badmonkey.agentspaces.console.*;
import ai.badmonkey.agentspaces.discovery.*;
import ai.badmonkey.agentspaces.identity.PeerIdentity;
import ai.badmonkey.agentspaces.peering.blocks.BlockExchange;
import ai.badmonkey.agentspaces.peering.membership.GroupMembership;
import ai.badmonkey.agentspaces.peering.node.PeerNode;
import ai.badmonkey.agentspaces.peering.transport.TcpTransport;
import ai.badmonkey.agentspaces.space.replicated.ReplicatedSpace;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public final class SpaceHost {
    /** The shared record type. Peers can also use their own types in this space. */
    public record Document(Map<String, Object> fields) {}

    public static void main(String[] args) throws Exception {
        String name = args[0], founding = args[1], bind = "127.0.0.1:" + args[2];
        int consolePort = Integer.parseInt(args[3]);
        var seeds = args[4].isEmpty() ? List.<PeerAdvertisement.Endpoint>of()
                : List.of(new PeerAdvertisement.Endpoint("tcp", args[4], 0));
        Document initial = args.length == 6
                ? new ObjectMapper().readValue("{\"fields\":" + args[5] + "}", Document.class) : null;
        var identity = PeerIdentity.generate();
        var id = GroupId.fromFounding(founding.getBytes(StandardCharsets.UTF_8));
        var group = new GroupAdvertisement("aspace://" + id.value(), identity.peerId(), id,
                Instant.EPOCH, Duration.ofDays(365), founding, GroupAdvertisement.MembershipPolicy.OPEN,
                ConflictStrategyType.LEASE_RACE, GroupAdvertisement.GossipParameters.defaults());
        var closed = new CountDownLatch(1);
        try (var node = PeerNode.builder(identity).build()) {
            node.listen(new TcpTransport(), bind);
            var runtime = node.joinGroup(group, new GroupMembership.Config(
                    Duration.ofSeconds(30), Duration.ofSeconds(2), 2), seeds);
            var codec = CborCodec.defaultCodec();
            var discovery = new DiscoveryService(runtime, new AdCache(codec, InstantSource.system()),
                    codec, identity.peerId());
            try (var space = ReplicatedSpace.builder(runtime, name, identity, "bb")
                         .blocks(new BlockExchange(runtime, codec)).advertise(discovery::publish).build();
                 var view = ConsoleView.builder().space(name, space, Document.class)
                         .members(() -> runtime.membership().allMembers()).discovery(discovery).build();
                 var console = FleetConsoleServer.builder(view).panel(new ConsolePanel() {
                     public String id() { return "entries"; }
                     public String title() { return name; }
                     public Object data() {
                         return space.readAll(Template.of(Document.class), 100).stream()
                                 .map(Document::fields).toList();
                     }
                 }).build()) {
                int http = console.start(consolePort);
                node.startTicking(Duration.ofMillis(250));
                if (initial != null) space.write(initial, Lease.of(Duration.ofHours(1)));
                System.out.printf("Space %s | group %s | tcp://%s | http://127.0.0.1:%d%n",
                        name, id.value(), bind, http);
                // Let try-with-resources close the console, space and node on SIGTERM or Ctrl-C.
                var stop = new CountDownLatch(1);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    stop.countDown();
                    try { closed.await(5, java.util.concurrent.TimeUnit.SECONDS); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }));
                stop.await();
            }
        } finally {
            closed.countDown();
        }
    }
}
