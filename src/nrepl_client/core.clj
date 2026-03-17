(ns nrepl-client.core
  (:require [nrepl.core :as nrepl]
            [rebel-readline.core :as rebel]
            [rebel-readline.clojure.line-reader :as clj-line-reader]
            [rebel-readline.clojure.service.local :as rebel-service]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn evaluate-input
  "Sends the code to the nREPL server and prints the responses."
  [client code]
  (let [responses (nrepl/message client {:op "eval" :code code})]
    (doseq [{:keys [out err value]} responses]
      (when out (print out))
      (when err (binding [*out* *err*] (print err)))
      (when value (println value)))
    (flush)))

(defn repl-loop
  "Starts the interactive Rebel Readline loop, forwarding input to nREPL."
  [client]
  (rebel/with-line-reader
    (clj-line-reader/create (rebel-service/create))
    (loop []
      (when-let [input (rebel/read-line "nrepl> ")]
        (when-not (str/blank? input)
          (if (= input ":quit")
            (println "Bye!")
            (do
              (evaluate-input client input)
              (recur))))))))

;; --- CLI Parsing ---

(def cli-options
  [["-p" "--port PORT" "Port number for the nREPL server"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a valid port number between 0 and 65536"]]
   ["-H" "--host HOST" "Host name or IP address"
    :default "127.0.0.1"]
   ["-h" "--help" "Show this help message"]])

(defn -main
  "Entry point for our standalone binary."
  [& args]
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)
      (do (println "Usage: nrepl-client [options]\n\nOptions:")
          (println summary)
          (System/exit 0))

      errors
      (do (println (str/join \newline errors))
          (System/exit 1))

      (not (:port options))
      (do (println "Error: --port is required.")
          (println "\nUsage: nrepl-client [options]\n\nOptions:")
          (println summary)
          (System/exit 1))

      :else
      (let [{:keys [host port]} options]
        (println (format "Connecting to nREPL at %s:%s..." host port))
        (try
          (with-open [conn   (nrepl/connect :host host :port port)
                      client (nrepl/client conn 1000)]
            (println "Connected! Type :quit to exit.")
            (repl-loop client))
          (catch Exception e
            (println "Failed to connect to nREPL server:" (.getMessage e))
            (System/exit 1)))))))
