(ns backend.handler
  (:require [backend.api :as api]
            [backend.static :as static]
            [kekkonen.cqrs :refer :all]
            [metosin.ring.util.cache :as cache]
            [metosin.transit.dates :as transit-dates]
            [palikka.core :as palikka]))

(defn ^:query ping [_]
  (success {:ping "Pong"}))

(defn api [env system]
  (cqrs-api {:swagger {:info {:title "Linkit"}}
             :swagger-ui {:path "/api-docs"
                          :validator-url nil}
             :core {:handlers {:foo {:ping #'ping}
                               :links 'backend.api}
                    :user {::api/load-link api/load-link
                           ::api/liked? api/require-liked}
                    :context (palikka/create-context system)}
             :mw {:format {:params-opts {:transit-json {:handlers transit-dates/readers}}
                           :response-opts {:transit-json {:handlers transit-dates/writers}}}}}))

(defn create-handler [env system]
  ; Re-create kekkonen handler for each request if in dev-mode
  (let [kekkonen (if (get-in env [:kekkonen :dev])
                   (fn [req] ((api env system) req))
                   (api env system))
        static (static/static-handler {:dev-tools? (get-in env [:ui :dev-tools])})]
    (-> (fn [req]
          (or (static req)
              (kekkonen req)))
        (cache/wrap-cache {:value cache/no-cache
                           :default? true}))))
