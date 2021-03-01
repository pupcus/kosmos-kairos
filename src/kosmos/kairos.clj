(ns kosmos.kairos
  (:require [clojure.data :as data]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [cronjure.core :as cron]
            [cronjure.definitions :as cron.def]
            [kosmos.kairos.util :as util]
            [kosmos.util :as u])
  (:import (java.util.concurrent TimeUnit Executors ScheduledThreadPoolExecutor)))

(def ^:private scheduler (atom nil))
(def ^:private jobs (ref {}))

(declare schedule-task)

(add-watch jobs :jobs (fn [k ref old new]
                        (let [[stopping starting _] (clojure.data/diff old new)]
                          (when starting
                            (log/debug "kairos: starting job(s): " (keys starting)))
                          (when stopping
                            (log/debug "kairos: stopping job(s): " (keys stopping))
                            ;; schedule next trigger for cron jobs
                            (doseq [[task-name {:keys [interval cron] :as task}] stopping :when cron]
                              (schedule-task @scheduler task))))))

(defn schedule-task [scheduler
                     {:keys [resolved-fn task-fn delay interval units cron crontype]
                      :or {units :seconds crontype :unix}
                      :as task}]
  (log/debugf "scheduling task: %s" task)
  (let [job-fn ^Runnable (let [task-name (str/join "-" [(name task-fn) (java.util.UUID/randomUUID)])]
                           (fn []
                             (dosync (commute jobs assoc task-name task))
                             (resolved-fn)
                             (dosync (commute jobs dissoc task-name))))]
    (cond

      cron
      (let [parsed-cron (cron/parse (util/cron-type crontype) cron)
            ned (util/next-execution parsed-cron)
            delay (cron/time-to-next-execution parsed-cron :millis)]
        (log/debugf "%s: schedulng next cron trigger for %s, approximately %s from now" (name task-fn) ned (util/human-readable delay))
        (.schedule scheduler job-fn delay (util/time-unit :millis)))

      interval
      (.scheduleWithFixedDelay scheduler
                               job-fn
                               (or delay interval)
                               interval
                               (util/time-unit units))
      :otherwise
      (.schedule scheduler job-fn (long (or delay 0)) (util/time-unit units)))))


(defn validate-tasks [tasks]
  (doseq [{:keys [delay interval cron] :as task} tasks]
    (when (and cron (or delay interval))
      (throw
       (ex-info
        "Cannot specify a cron expression along with either delay or interval for a single task"
        {:task task})))))

(defrecord SchedulerComponent []
  component/Lifecycle
  (start [{:keys [poolsize tasks] :as component}]
    (validate-tasks tasks)

    (let [task-count (count tasks)
          poolsize (max (or poolsize 0) (inc task-count))]
      (log/infof "Initializing cron component: [%s] threads in pool and [%s] tasks" poolsize task-count)
      (let [s ^ScheduledThreadPoolExecutor (ScheduledThreadPoolExecutor. poolsize)]
        (log/info "scheduler initialized")

        (log/debug "scheduler atom reset")
        (reset! scheduler s)

        ;; don't start any pending jobs after we shutdown
        (.setExecuteExistingDelayedTasksAfterShutdownPolicy s false)

        (log/infof "scheduling [%s] tasks with scheduler ..." task-count)
        (doseq  [{:keys [task-fn] :as task} tasks]
          (schedule-task s (assoc task :resolved-fn (u/resolve-symbol task-fn))))
        (log/infof "scheduler running and scheduled with [%s] tasks" task-count)
        (assoc component :scheduler s))))

  (stop [{:keys [^ScheduledThreadPoolExecutor scheduler wait-on-shutdown-secs] :or {wait-on-shutdown-secs 5}:as component}]
    (log/info "Stopping scheduler component.")
    (.shutdown scheduler)
    (try
      (let [terminated (.awaitTermination scheduler wait-on-shutdown-secs TimeUnit/SECONDS)]
        (if-not terminated
          (-> component
              (assoc :pending (.shutdownNow scheduler))
              (dissoc :scheduler))
          (dissoc component :scheduler)))
      (catch InterruptedException e
        (log/error "scheduler thread interrupted while waiting for shutdown!")
        component))))
