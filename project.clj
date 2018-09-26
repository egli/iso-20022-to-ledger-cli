(defproject iso-20022-to-ledger-cli "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.xml "0.2.0-alpha5"]
                 [org.clojure/data.zip "0.1.2"]
                 [clojure.java-time "0.3.2"]]
  :main ^:skip-aot iso-20022-to-ledger-cli.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
