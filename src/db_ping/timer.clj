(ns db-ping.timer
  (:import (java.util Timer TimerTask)))

(defn timer
  "Creates a java.util.Timer where tasks can be scheduled."
  ([] (Timer. false))
  ([name] (Timer. name false)))

(defn run-task!
  "Executes a task on a Timer."
  [task period timer & {:keys [on-exception]}]
  {:pre [(fn? task)
         (and (> period 0))
         (instance? Timer timer)
         (or (nil? on-exception) (fn? on-exception))]
   :post [(instance? Timer %)]}
  (let [task (proxy [TimerTask] []
               (run []
                 (if (nil? on-exception)
                   (task)
                   (try
                     (task)
                     (catch Exception e
                       (on-exception e))))))
        delay 0]
    (.schedule timer task delay (long period))
    timer))

(defn cancel!
  "Ends all tasks scheduled on a Timer."
  [^Timer timer]
  (.cancel timer))
