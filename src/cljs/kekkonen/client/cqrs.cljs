(ns kekkonen.client.cqrs
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs-http.core :as http-core]
            [cljs.core.async :as async :refer [<!]]
            [clojure.string :as string]
            [metosin.transit :as transit]))

(defn action->uri [action]
  {:pre [(keyword? action)]}
  (string/replace (string/join "/" ((juxt namespace name) action)) #"\." "/"))

(defn json? [content-type]
  (.startsWith content-type "application/json"))

; Ideas:
; - Coerce responses based on Schema (json)

(defn create-client [{:keys [base-uri] :as opts}]
  (go
    ; FIXME: Kekkonen should return vector of handlers instead of sequence
    (let [resp (<! (http/get (str base-uri "kekkonen/all-handlers") {:accept "application/transit+json"}))
          content-type (get-in resp [:headers "content-type"])
          handlers (:body resp)
          handlers (cond
                     ; Coerce JSON response
                     ; Use Schema?
                     (json? content-type)
                     (map (fn [{:keys [type action] :as handler}]
                            (assoc handler :type (keyword type) :action (keyword action)))
                          handlers)

                     :else
                     handlers)]
      (merge opts
             {:handlers (vec handlers)
              :handlers-by-action (into {} (map (juxt :action identity) handlers))}))))

(defn get-handler [client action]
  (or (get (:handlers-by-action client) action)
      (throw (js/Error. (str "Tried to retrieve non-existing command: " action)))))

(declare invoke query command)

(defn wrap-results [handler]
  (fn [req]
    (let [{:keys [kekkonen method]} req
          {:keys [action data client]} kekkonen
          {:keys [result-handler]} client]
      (if (= :post method)
        (async/map (fn [resp]
                     (doseq [[action data] (:changed (:body resp))]
                       (go (result-handler {:query [action data]
                                            :data (:body (<! (query client action data)))})))
                     resp)
                   [(handler req)])
        (async/map (fn [resp]
                     (result-handler {:query [action data]
                                      :data (:body resp)})
                     resp)
                   [(handler req)])))))

(defn wrap-request [req]
  (-> req
      http/wrap-request
      wrap-results))

(def request (wrap-request http-core/request))

(defn invoke
  ([client action]
   (invoke client action nil))
  ([client action data]
   (request
     (let [handler (get-handler client action)
           command? (= :command (:type handler))]
       {:uri (str (:uri client) "/" (action->uri action))
        :method (if command? :post :get)
        (if command? :transit-params :query-params) data
        :headers {"kekkonen.mode" "invoke"}
        :accept "application/transit+json"
        :kekkonen {:client client
                   :action action
                   :data data}
        ; FIXME:
        :transit-opts {:encoding-opts {:handlers transit/writers}
                       :decoding-opts {:handlers transit/readers}}}))))

(def command invoke)
(def query invoke)
