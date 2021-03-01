(ns kosmos.kairos.util
  (:require
   [clojure.string :as str]
   [cronjure.core :as cron]
   [cronjure.definitions :as cron.def])
  (:import (java.util.concurrent TimeUnit)
           (com.cronutils.model CronType)))

(defn human-readable
  ([start stop] (human-readable (- stop start)))
  ([elapsed]
   (let [hours    (long (/ elapsed 3600000))
         minutes  (long (- (/ elapsed 60000) (* hours 60)))
         seconds  (long (- (/ elapsed 1000) (+ (* hours 3600) (* minutes 60))))
         millis   (long (- elapsed (+ (* hours 3600000) (* minutes 60000) (* seconds 1000))))
         results (reduce
                  (fn [res [k value :as item]]
                    (if (> value 0)
                      (conj res item)
                      res))
                  []
                  [[:hrs hours] [:mins minutes] [:secs seconds] [:ms millis]])]

     (str elapsed " ms total ["
          (str/join
           " "
           (mapv
            (fn [[k v]]
              (str v " " (name k)))
            results))
          "]"))))


(defn ^TimeUnit time-unit
  ([] TimeUnit/SECONDS)
  ([units]
   (if units
     (let [units (keyword (str/lower-case (name units)))]
       (case units
         (:s :sec :secs :second :seconds)
         TimeUnit/SECONDS

         (:d :day :days :sols)
         TimeUnit/DAYS

         (:h :hr :hrs :hour :hours)
         TimeUnit/HOURS

         (:microsecond :microseconds :micro :micros :micro-second :micro-seconds)
         TimeUnit/MICROSECONDS

         (:millisecond :milliseconds :milli :millis :milli-second :milli-seconds)
         TimeUnit/MILLISECONDS

         (:nanosecond :nanoseconds :nano :nanos :nano-second :nano-seconds)
         TimeUnit/NANOSECONDS

         (:m :min :mins :minute :minutes)
         TimeUnit/MINUTES

         TimeUnit/SECONDS))
     TimeUnit/SECONDS)))

(defn ^CronType cron-type
  ([] cron.def/unix)
  ([type]
   (if type
     (let [type (keyword (str/lower-case (name type)))]
       (case type
         (:q :qtz :qrtz :quartz)
         cron.def/quartz

         :cron4j
         cron.def/cron4j

         (:unix :bsd :linux :osx)
         cron.def/unix

         :spring
         cron.def/spring

         ;; otherwise
         cron.def/unix))
     cron.def/unix)))

(defn next-execution
  ([cron-instance] (cron/next-execution-date cron-instance))
  ([cron-instance execution-time]
   (-> (com.cronutils.model.time.ExecutionTime/forCron cron-instance)
       (.nextExecution execution-time)
       .get)))

(defn prev-execution
  ([cron-instance] (cron/last-execution-date cron-instance))
  ([cron-instance execution-time]
   (-> (com.cronutils.model.time.ExecutionTime/forCron cron-instance)
       (.lastExecution execution-time)
       .get)))

(defn next-executions
  ([cron-instance]
   (next-executions cron-instance (next-execution cron-instance)))
  ([cron-instance execution-time]
   (lazy-seq
    (cons
     execution-time
     (next-executions cron-instance
                      (next-execution cron-instance execution-time))))))

(defn prev-executions
  ([cron-instance]
   (prev-executions cron-instance (prev-execution cron-instance)))
  ([cron-instance execution-time]
   (lazy-seq
    (cons
     execution-time
     (prev-executions cron-instance
                      (prev-execution cron-instance execution-time))))))
