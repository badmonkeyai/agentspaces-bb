# Security policy

Report suspected vulnerabilities privately to oss [at] badmonkey.ai. For defects in AgentSpaces core, use the [core project's private reporting channel](https://github.com/badmonkeyai/AgentSpaces/security/advisories/new).

The first release hosts an open development group over plain TCP. Both listeners bind to loopback. Local processes can join the group or read the console. Use a trusted development machine and trusted seeds. The hosted console enables read-only operations. A fresh identity and in-memory replica are created at each start.
