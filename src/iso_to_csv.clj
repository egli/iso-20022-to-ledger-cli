(ns iso-to-csv
  (:require [clojure.data.csv :as csv]
            [iso-to-ledger :as ledger]))

(defn -main
  "Convert [ISO 20022](https://en.wikipedia.org/wiki/ISO_20022) to CSV"
  [& args]
  (doseq [file args]
    (let [entries (-> file ledger/read-file :entries)
          header (->> entries first keys (map name))]
      (csv/write-csv *out* (conj (->> entries (map vals)) header)))))
