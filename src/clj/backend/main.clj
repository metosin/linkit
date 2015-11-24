(ns backend.main
  (:require [reloaded.repl :refer [go set-init!]])
  (:gen-class))

(defn init
  ([] (init nil))
  ([opts]
   (require 'backend.system)
   ((resolve 'backend.system/new-system) opts)))

(defn setup-app! [opts]
  (set-init! #(init opts)))

(defn -main [& args]
  (setup-app! nil)
  (go))
