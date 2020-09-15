(ns db-ping.main
  (:require [cli-matic.core :refer [run-cmd]]
            [cljstache.core :refer [render]]
            [clojure.core.async :as async]
            [db-ping.timer :refer [timer run-task!]]
            [expound.alpha :as expound]
            [next.jdbc :as jdbc]))

(def da-chan (async/chan))

(defn shutdown-hook []
  (async/close! da-chan))

(def clock (timer "Run DB queries"))

(defn execute-db-query [jdbc-uri query]
  (let [start (System/currentTimeMillis)]
    {:result   (jdbc/execute-one! (jdbc/get-datasource jdbc-uri) [query])
     :duration (- (System/currentTimeMillis) start)}))

(defn do-the-db-thing [jdbc-uri query]
  (let [result       (execute-db-query jdbc-uri query)
        now          (java.time.Instant/now)
        iso-time     (.toString now)
        unix-time-ms (.toEpochMilli now)
        unix-time    (int (/ unix-time-ms 1000))]
    (async/put! da-chan
                {:iso-time       iso-time
                 :query-duration (:duration result)
                 :query-result   (:result result)
                 :unix-time      unix-time
                 :unix-time-ms   unix-time-ms})))

(defn- exception-handler
  "A 1-arity function that does nothing"
  [^Throwable thrown]
  (binding [*out* *err*]
    (println "An exception occurred:" (.getMessage thrown))))

(defn ping [{:keys [jdbc-uri period query template]}]
  (run-task! #(do-the-db-thing jdbc-uri query) (* period 1000) clock :on-exception exception-handler)
  (while true
    (when-let [d (async/<!! da-chan)]
      (println (render template d)))))

(def CONFIGURATION
  {:app      {:command     "db-pinger"
              :description "Like ping(1) but for databases"
              :version     "0.0.0"}
   :commands [{:command     "ping" :short "p"
               :description "Repeatedly connects to & queries a database"
               :opts        [{:option  "jdbc-uri"
                              :type    :string
                              :default :present}
                             {:option  "query"
                              :as      "This query will be executed during each ping"
                              :type    :string
                              :default "SELECT 1;"}
                             {:option  "period"
                              :as      "Seconds between connections"
                              :type    :float
                              :default 1.0}
                             {:option  "template"
                              :as      "mustache(5) string template to format output as when printing"
                              :type    :string
                              :default "{{iso-time}},{{query-duration}},{{query-result}}"}]
               :runs        ping
               :on-shutdown shutdown-hook}]
   })

(defn -main [& args]
  (run-cmd args CONFIGURATION))
