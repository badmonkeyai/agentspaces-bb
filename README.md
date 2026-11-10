# AgentSpaces Babashka

AgentSpaces bindings for Babashka. Query an AgentSpaces fleet from a shell, or start a peer that provides a named replicated space. Queries run entirely in Babashka. Hosting launches the AgentSpaces 0.6.0 JVM libraries in a separate JVM: `bb as serve` resolves the classpath from `deps.edn` with the Clojure CLI, then runs `host/agentspaces/bb/SpaceHost.java` through the JDK's single-file source launcher.

## Start a space

Install Babashka (tested with 1.12.217). Hosting also requires JDK 21+, Clojure CLI, and access to the Maven artifacts in deps.edn. 

```sh
bb as serve orders --entry '{"orderId":"o-1","item":"kite"}'
```

From another terminal:

```sh
bb as spaces
bb as entries entries '{"item":"kite"}'
```

The first `entries` is the query command and the second names the host's built-in panel. It exposes the fields of up to 100 readable `agentspaces.bb.SpaceHost$Document` records. Other AgentSpaces peers may use their own record types in the same space. The optional startup document has a one-hour lease.

Start another peer on the same machine to replicate the space:

```sh
bb as serve orders --seed 127.0.0.1:7701 --port 7702 --console-port 8081
AGENTSPACES_URL=http://127.0.0.1:8081 bb as entries entries
```

Each host uses a fresh peer identity. Match the space name and group founding text across peers. The default founding text is `agentspaces-bb-v1`. Override it with `--group TEXT` on both hosts. Both listeners bind to loopback. Stop with Ctrl-C. Records are held in memory and disappear when all replicas stop or the lease expires.

Use `bin/agentspaces-bb` from an extracted distribution, or put its bin directory on PATH. See [CONTRIBUTING.md](CONTRIBUTING.md) for testing and packaging.

## Query a running fleet

Set the console base URL, including any reverse-proxy prefix. The default is `http://127.0.0.1:8080`.

```sh
export AGENTSPACES_URL=http://127.0.0.1:8080
bb as overview
bb as peers
bb as ads
bb as spaces
bb as space orders
bb as panels
bb as panel order-entries
bb as entries order-entries '{"item":"kite"}'
```

Each command emits one JSON value on stdout. Errors go to stderr with a nonzero exit status. An empty match returns `[]` successfully.

`space orders` returns the console's statistics for the watched space. For tuple values, register a panel on the host that returns an array of records. The example uses a non-consuming `readAll` for the Order type, capped at 100. The client filters that snapshot by exact top-level field values. Omit the filter to see the whole snapshot. Each request reads the current panel data.

## Run the example

The example host needs JDK 21+ and Clojure CLI with access to the AgentSpaces 0.6.0 Maven artifacts. From the repository root, run this in one terminal:

```sh
cd examples
java -cp "$(clojure -Spath)" OrdersConsole.java
```

From another terminal in the repository root, run `bb as entries order-entries '{"item":"kite"}'`. The seeded local space returns `[{"orderId":"o-1","item":"kite"}]`. To query a replicated fleet space, register the same panel on its existing console and bind it to the host's space and record class.

## Use from a Babashka script

With this project's `src` on the Babashka classpath:

```clojure
(require '[agentspaces.bb.client :as as])
(as/query "http://127.0.0.1:8080" :spaces)
(as/entries "http://127.0.0.1:8080" "order-entries" {"item" "kite"})
```

Run the client contract tests with `bb test/client_test.clj`.

## License

[Apache License 2.0](LICENSE). Copyright 2026 Bad Monkey, Inc.
