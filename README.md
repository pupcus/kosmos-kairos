# kosmos-kairos

A system component to add simple scheduling to kosmos. Schedulling can be defined by a simple delay (one execution), a delay with interval (repeating executions), or a cron expression (more complicated, repeating executions). Cron expressions can be a 'unix', 'quartz', 'spring', or 'cron4j' expression (based on the cron-utils java library parsing). The default cron format is 'unix'.

## Usage

add something like this to your kosmos .edn files:

```Clojure
:jobs
{
 :kosmos/init :kosmos.kairos/SchedulerComponent

 :wait-on-shutdown-secs 5

 :tasks [{:task-fn :some.name.space/task2
          :delay 1
          :interval 3
          :units :seconds}

         {:task-fn :some.name.space/task4
          :cron "*/1 * * * *"}

         {:task-fn :some.name.space/task5
          :cron "*/10 * * * * ?"
          :crontype :quartz}

         {:task-fn :some.name.space/task1
          :cron "*/20 * * * * *"
          :crontype :spring}

         {:task-fn :some.name.space/task3
          :delay 30
          :units :seconds}

         {:task-fn :some.name.space/task6
          :cron "*/1 * * * *"
          :crontype :cron4j}]
 }
```

The `:task-fn` key will be resolved as a var and executed at the appointed time.

## License

Kosmos is distributed under the [Eclipse Public License](http://opensource.org/licenses/eclipse-1.0.php), the same as Clojure.
