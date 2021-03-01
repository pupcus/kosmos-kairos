(ns kosmos.kairos-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.tools.logging :as log]
            [kosmos :refer [map->system start! stop! system]]
            [kosmos.kairos :refer :all]))

(def calls (atom {}))

(defn task1 []
  (log/info "running task1")
  (let [n (get @calls :task1 0)]
    (swap! calls assoc :task1 (inc n)))
  (Thread/sleep 2000)
  (log/info "task1 complete"))

(defn task2 []
  (log/info "runnnig task2")
  (let [n (get @calls :task2 0)]
    (swap! calls assoc :task2 (inc n)))
  (Thread/sleep 2000)
  (log/info "task2 complete"))

(defn task3 []
  (log/info "runnnig task3")
  (let [n (get @calls :task3 0)]
    (swap! calls assoc :task3 (inc n)))
  (Thread/sleep 5000)
  (log/info "task3 complete"))

(defn task4 []
  (log/info "runnnig task4")
  (let [n (get @calls :task4 0)]
    (swap! calls assoc :task4 (inc n)))
  (log/info "task4 complete"))

(defn task5 []
  (log/info "runnnig task5")
  (let [n (get @calls :task5 0)]
    (swap! calls assoc :task5 (inc n)))
  (log/info "task5 complete"))

(defn task6 []
  (log/info "runnnig task6")
  (let [n (get @calls :task6 0)]
    (swap! calls assoc :task6 (inc n)))
  (Thread/sleep 2000)
  (log/info "task6 complete"))

(def config {
             :jobs
             {:kosmos/init :kosmos.kairos/SchedulerComponent

              :tasks [
                      {:task-fn :kosmos.kairos-test/task2
                       :delay 1
                       :interval 3
                       :units :seconds}

                      {:task-fn :kosmos.kairos-test/task4
                       :cron "*/1 * * * *"}

                      {:task-fn :kosmos.kairos-test/task5
                       :cron "*/10 * * * * ?"
                       :crontype :quartz}

                      {:task-fn :kosmos.kairos-test/task1
                       :cron "*/20 * * * * *"
                       :crontype :spring}

                      {:task-fn :kosmos.kairos-test/task3
                       :delay 30
                       :units :seconds}

                      {:task-fn :kosmos.kairos-test/task6
                       :cron "*/1 * * * *"
                       :crontype :cron4j}
                      ]

              :wait-on-shutdown-secs 5
              }
             })


(deftest test-scheduler-component
  (start! (map->system config))
  (try
    (doseq [k (keys system)]
      (log/infof "{%s %s}" k (pr-str (k system))))

    (log/info "waiting on scheduler")
    (Thread/sleep 120000)

    (is (=
         {:task1 6
          :task2 24
          :task3 1
          :task4 2
          :task5 12
          :task6 2}
         @calls))
    (catch Exception e
      (log/error e e))
    (finally
      (stop!))))
