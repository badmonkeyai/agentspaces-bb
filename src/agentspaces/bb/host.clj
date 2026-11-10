;; Copyright 2026 Bad Monkey, Inc.
;; Licensed under the Apache License, Version 2.0.
(ns agentspaces.bb.host
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def usage
  "bb as serve [SPACE] [--group FOUNDING-TEXT] [--port PORT] [--console-port PORT]
            [--seed HOST:PORT] [--entry JSON-OBJECT]
Defaults: main, agentspaces-bb-v1, TCP 7701, console 8080 (both loopback).")

(defn- port [s]
  (let [n (parse-long s)]
    (when-not (and n (<= 1 n 65535))
      (throw (ex-info (str "Invalid port " s) {})))
    n))

(defn host-args
  "Validate the listener arguments before resolving any JVM dependencies."
  [args]
  (let [named? (and (seq args) (not (str/starts-with? (first args) "--")))
        space (if named? (first args) "main")
        options (if named? (rest args) args)]
    (when (or (str/blank? space) (odd? (count options))) (throw (ex-info usage {})))
    (let [pairs (partition 2 options)
          opts (into {} (map vec pairs))
          group (get opts "--group" "agentspaces-bb-v1")
          seed (get opts "--seed" "")
          tcp (port (get opts "--port" "7701"))
          http (port (get opts "--console-port" "8080"))]
      (when-not (and (every? #{"--group" "--seed" "--port" "--console-port" "--entry"} (keys opts))
                     (= (count opts) (count pairs)) (not (str/blank? group)) (not= tcp http))
        (throw (ex-info usage {})))
      (when (contains? opts "--seed")
        (let [[_ p] (re-matches #"[^:/\s]+:([0-9]+)" seed)]
          (when-not p (throw (ex-info "Expected --seed HOST:PORT" {})))
          (port p)))
      (cond-> [space group (str tcp) (str http) seed]
        (contains? opts "--entry")
        (conj (let [value (json/parse-string-strict (opts "--entry"))]
                (when-not (map? value) (throw (ex-info "--entry requires a JSON object" {})))
                (json/generate-string value)))))))

(defn serve!
  "Replace the CLI process with a foreground AgentSpaces peer."
  [args]
  (let [argv (host-args args)
        cp (:out (process/shell {:out :string} "clojure" "-Srepro" "-Spath"))
        java (or (fs/which "java") (throw (ex-info "Hosting requires JDK 21+" {})))]
    (process/exec (into [(str java) "-cp" (str/trim cp) "host/agentspaces/bb/SpaceHost.java"] argv))))
