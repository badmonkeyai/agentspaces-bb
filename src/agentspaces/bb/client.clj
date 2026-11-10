;; Copyright 2026 Bad Monkey, Inc.
;; Licensed under the Apache License, Version 2.0.
(ns agentspaces.bb.client
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]))

(defn- segment [id]
  (when (or (str/blank? id) (#{"." ".."} id) (str/includes? id "/"))
    (throw (ex-info "Expected one non-empty resource name" {})))
  (str/replace (java.net.URLEncoder/encode id "UTF-8") "+" "%20"))

(defn query
  "Fetch one Fleet Console document. JSON objects become string-keyed maps."
  [base-url resource & [id]]
  (let [path (case resource
               (:overview :peers :ads :spaces :panels) (name resource)
               :space (str "spaces/" (segment id))
               :panel (str "panels/" (segment id) "/data"))
        url (str (str/replace base-url #"/+$" "") "/api/v1/" path)
        {:keys [status body]} (http/get url {:headers {"Accept" "application/hal+json, application/json"}
                                            :timeout 5000 :throw false})]
    (when-not (= status 200)
      (throw (ex-info (str "Fleet Console returned HTTP " status " for " path)
                      {:status status})))
    (json/parse-string-strict body)))

(defn entries
  "Select records by exact top-level field values within a published panel snapshot."
  [base-url panel fields]
  (let [rows (query base-url :panel panel)]
    (when-not (and (sequential? rows) (every? map? rows))
      (throw (ex-info "The panel must expose an array of entry objects" {})))
    (filterv #(= fields (select-keys % (keys fields))) rows)))
