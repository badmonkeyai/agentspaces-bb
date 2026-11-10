;; Copyright 2026 Bad Monkey, Inc.
;; Licensed under the Apache License, Version 2.0.
(ns agentspaces.bb.cli
  (:require [agentspaces.bb.client :as client]
            [agentspaces.bb.host :as host]
            [cheshire.core :as json]))

(def usage
  (str host/usage "\n\nbb as overview|peers|ads|spaces|panels
bb as space NAME
bb as panel ID
bb as entries PANEL [JSON-FIELD-MAP]
Set AGENTSPACES_URL to the console base URL (default http://127.0.0.1:8080)."))

(defn run [url [command & args]]
  (let [op (keyword command)]
    (cond
      (and (#{:overview :peers :ads :spaces :panels} op) (empty? args))
      (client/query url op)

      (and (#{:space :panel} op) (= 1 (count args)))
      (client/query url op (first args))

      (and (= :entries op) (<= 1 (count args) 2))
      (let [fields (json/parse-string-strict (or (second args) "{}"))]
        (when-not (map? fields)
          (throw (ex-info "Expected a JSON object for the field filter" {})))
        (client/entries url (first args) fields))

      :else (throw (ex-info usage {})))))

(defn -main [& args]
  (if (or (empty? args) (= ["--help"] args))
    (println usage)
    (try
      (if (= "serve" (first args))
        (if (= ["--help"] (rest args)) (println host/usage) (host/serve! (rest args)))
        (println (json/generate-string
                (run (or (System/getenv "AGENTSPACES_URL") "http://127.0.0.1:8080") args))))
      (catch Exception e
        (binding [*out* *err*] (println (ex-message e)))
        (System/exit 1)))))
