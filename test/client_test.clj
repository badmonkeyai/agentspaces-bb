;; Copyright 2026 Bad Monkey, Inc.
;; Licensed under the Apache License, Version 2.0.
(require '[agentspaces.bb.cli :as cli]
         '[agentspaces.bb.host :as host]
         '[babashka.http-client :as http]
         '[clojure.test :refer [deftest is run-tests]])

(deftest tuple-query
  (with-redefs [http/get (fn [url opts]
                           (is (= "http://fleet/api/v1/panels/order%20list/data" url))
                           (is (false? (:throw opts)))
                           {:status 200 :body "[{\"item\":\"kite\"},{\"item\":null},{}]"})]
    (let [run #(cli/run "http://fleet/" ["entries" "order list" %])]
      (is (= [{"item" "kite"}] (run "{\"item\":\"kite\"}")))
      (is (= [{"item" nil}] (run "{\"item\":null}")))
      (is (= [] (run "{\"item\":\"boat\"}"))))))

(deftest failures
  (with-redefs [http/get (constantly {:status 404})]
    (is (thrown-with-msg? Exception #"HTTP 404" (cli/run "http://fleet" ["space" "missing"]))))
  (is (thrown? Exception (cli/run "unused" ["entries" "orders" "[]"])))
  (is (thrown? Exception (cli/run "unused" ["space" ".."]))))

(deftest serve-options
  (is (= ["main" "agentspaces-bb-v1" "7701" "8080" ""] (host/host-args [])))
  (is (= ["orders" "shop-v1" "7712" "8082" "localhost:7711" "{\"id\":1}"]
         (host/host-args ["orders" "--group" "shop-v1" "--port" "7712"
                          "--console-port" "8082" "--seed" "localhost:7711"
                          "--entry" "{\"id\":1}"])))
  (doseq [args [["--port" "0"] ["--port" "8080"] ["--port"] ["--seed" "abc"]
                ["--entry" "[]"] ["--foo" "x"] ["--port" "123" "--port" "456"]]]
    (is (thrown? Exception (host/host-args args)))))

(let [{:keys [fail error]} (run-tests)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
