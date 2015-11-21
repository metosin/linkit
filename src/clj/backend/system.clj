(ns backend.system
  (:require [com.stuartsierra.component :as component :refer [using]]
            [maailma.core :as m]
            [palikka.core :refer [providing]]
            [palikka.handler :refer [wrap-env wrap-context]]
            [palikka.components.http-kit :as http-kit]
            [palikka.components.nrepl :as nrepl]
            [palikka.components.mongo :as mongo]
            [backend.handler :refer [create-handler]]))

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
