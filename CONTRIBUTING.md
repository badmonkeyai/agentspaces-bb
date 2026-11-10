# Contributing

AgentSpaces Babashka is licensed under Apache 2.0. Follow the [Code of Conduct](CODE_OF_CONDUCT.md) and sign off each commit with `git commit -s` under the [Developer Certificate of Origin](https://developercertificate.org/). Report vulnerabilities through [SECURITY.md](SECURITY.md).

Run `bb test` for the CLI and client. Run `bb smoke` with JDK 21+, Clojure CLI, and free loopback ports 17701, 17702, 18081 and 18082. The test starts two real hosts, writes a document on the joining peer, reads it from the founding peer, and stops both.

## Prepare a release

Update CHANGELOG.md and the archive version in bb.edn. Run both test tasks, then `bb package`. Extract the archive into an empty directory, run `bin/agentspaces-bb --help`, and repeat the smoke test there. Verify Maven dependencies resolve with a clean cache before publishing. Tag the reviewed commit and attach the source archive to its release.
