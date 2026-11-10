#!/bin/sh
# Copyright 2026 Bad Monkey, Inc. Licensed under the Apache License, Version 2.0.
set -eu
cd "$(dirname "$0")/.."
logs=$(mktemp -d)
first= second=
cleanup() {
    for pid in "$first" "$second"; do
        if [ -n "$pid" ]; then kill "$pid" 2>/dev/null || true; wait "$pid" 2>/dev/null || true; fi
    done
    rm -rf "$logs"
}
trap cleanup EXIT
trap 'exit 1' INT TERM
wait_for() {
    pid=$1
    shift
    for _ in $(seq 1 60); do
        if ! kill -0 "$pid" 2>/dev/null; then break; fi
        if "$@" 2>/dev/null; then return 0; fi
        sleep 1
    done
    cat "$logs"/*.log >&2
    return 1
}
group="bb-smoke-$$"
bin/agentspaces-bb serve orders --group "$group" --port 17701 --console-port 18081 >"$logs/first.log" 2>&1 &
first=$!
wait_for "$first" grep -q 'Space orders' "$logs/first.log"
bin/agentspaces-bb serve orders --group "$group" --port 17702 --console-port 18082 --seed 127.0.0.1:17701 --entry '{"source":"second"}' >"$logs/second.log" 2>&1 &
second=$!
replicated() {
    rows=$(AGENTSPACES_URL=http://127.0.0.1:18081 bin/agentspaces-bb entries entries '{"source":"second"}') &&
        [ "$rows" = '[{"source":"second"}]' ]
}
wait_for "$second" replicated
echo "The first peer read the second peer's document."
