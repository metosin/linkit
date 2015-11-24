(ns backend.handler
  (:require [backend.api :as api]
            [backend.static :as static]
            [kekkonen.cqrs :refer :all]
            [metosin.ring.util.cache :as cache]
            [metosin.transit :as transit]
            [palikka.core :as palikka]))

(defn ^:query ping [_]
  (success {:ping "Pong"}))

(defn api [env system]
  (cqrs-api {:swagger {:info {:title "Linkit"}}
             :swagger-ui {:path "/api-docs"}
             :core {:handlers {:foo {:ping #'ping}
                               :links 'backend.api}
                    :user {::api/load-link api/load-link
                           ::api/liked? api/require-liked}
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
