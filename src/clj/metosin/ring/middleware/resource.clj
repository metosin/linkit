(ns ring.middleware.resource
  "Middleware for serving static resources."
  (:require [ring.util.codec :as codec]
            [ring.util.response :as response]
            [ring.util.request :as request]
            [ring.middleware.head :as head]))

(defn resource-request
  [request prefix & [{:keys [root loader]}]]
  (if (#{:head :get} (:request-method request))
    (let [path (subs (codec/url-decode (request/path-info request)) 1)]
      (if (re-matches path)
        (-> (response/resource-response path {:root root :loader loader})
            (head/head-response request))))))

(defn wrap-resource
  [handler prefix & [{:keys [root loader]}]]
  (fn [request]
    (or (resource-request request prefix {:root root :loader loader})
        (handler request))))
