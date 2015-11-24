(ns backend.system
  (:require [backend.handler :refer [create-handler]]
            [com.stuartsierra.component :as component :refer [using]]
            [maailma.core :as m]
            [palikka.components.http-kit :as http-kit]
            [palikka.components.mongo :as mongo]
            [palikka.components.nrepl :as nrepl]
            [palikka.core :refer [providing]])
  (:import [org.joda.time DateTimeZone]))

; Default time-zone is UTC. Life is simple.
(DateTimeZone/setDefault (DateTimeZone/forID "UTC"))

(defn new-system [override]
  (let [env (m/build-config
              (m/resource "config-defaults.edn")
              (m/file "./config-local.edn"))]
    (component/map->SystemMap
      (cond-> {:mongo        (-> (mongo/create (:mongo env))
                                 (providing [:conn :db]))
               :http         (-> (http-kit/create (:http env) {:fn (partial create-handler env)})
                                 ; For context
                                 (using [:mongo]))}
        (:nrepl env) (assoc :nrepl (nrepl/create (:nrepl env)))))))
