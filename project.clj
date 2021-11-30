(defproject kosmos/kosmos-kairos "0.0.2"

  :description "simple task scheduling for kosmos"

  :url "https://github.com/pupcus/kosmos-kairos"

  :scm {:url "git@github.com:pupcus/kosmos-kairos.git"}

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :signing {:gpg-key "FCA46A30FEEE7E10"}

  :dependencies [[kosmos "0.0.13"]
                 [cronjure "0.1.1" :exclusions [com.cronutils/cron-utils]]
                 [com.cronutils/cron-utils "9.1.6"]]

  :profiles {:dev {:resource-paths ["dev-resources"]
                   :dependencies [[org.clojure/clojure "1.10.3"]
                                  [org.slf4j/slf4j-log4j12 "1.7.32"]]}}

  :deploy-repositories {"releases" {:url "https://clojars.org/repo" :creds :gpg :sign-releases false}
                        "snapshots" {:url "https://clojars.org/repo" :creds :gpg :sign-releases false}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  #_#_:global-vars {*warn-on-reflection* true
                    *assert* false})
