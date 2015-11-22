(ns backend.handler
  (:require [metosin.ring.util.cache :as cache]
            [palikka.core :as palikka]
            [backend.static :as static]
            [kekkonen.cqrs :refer :all]
            [backend.api :as api]
            [metosin.transit :as transit]))

(defn ^:query ping [_]
  (success {:ping "Pong"}))

(defn api [env system]
  (cqrs-api {:swagger {:info {:title "Linkit"}}
             :swagger-ui {:path "/api-docs"}
             :core {:handlers {:foo {:ping #'ping}
                               :links 'backend.api}
                    :context (palikka/create-context system)}
             :mw {:format {:params-opts {:transit-json {:handlers transit/readers}}
                           :response-opts {:transit-json {:handlers transit/writers}}}}}))

(defn create-handler [env system]
  (let [kekkonen (if (get-in env [:kekkonen :dev])
                   (fn [req] ((api env system) req))
                   (api env system))
        static (static/static-handler env)]
    (-> (fn [req]
          (or (static req)
              (kekkonen req)))
        (cache/wrap-cache {:value cache/no-cache
                           :default? true}))))
