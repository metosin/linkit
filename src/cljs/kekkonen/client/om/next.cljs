(ns kekkonen.client.om.next
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! chan put!]]
            [kekkonen.client.cqrs :as cqrs]
            [ring.util.http-predicates :refer [ok?]]
            [om.next :as om]))

(defn remote-loop [ch client]
  (go
    (loop [[type x cb] (<! ch)]
      (case type
        :query
        (let [[query params] (if (coll? x) x [x])
              {:keys [body] :as res} (<! (cqrs/query client query (case query
                                                                    :links/by-id {:link-id params}
                                                                    nil)))]
          (if (ok? res)
            ; FIXME: This should probably be declarative somehow?
            (case query
              :links/all (cb {:links/by-id (into {} (map (juxt :_id identity) body))
                              :links/all (into [] (map (fn [{:keys [_id]}] [:links/by-id _id]) body))})
              :links/by-id (cb {:links/by-id {params body}}))))

        :command
        (let [[command params] x]
          (<! (cqrs/command client (keyword command) params))))

      (recur (<! ch)))))

(defn send-to-chan [ch client]
  (fn [{:keys [query remote state] :as env} cb]
    ; If same transaction has both command and query, commands should be executed first
    (when remote
      (put! ch [:command (first remote) cb]))
    (when query
      (let [{[x] :children} (om/query->ast query)]
        (put! ch [:query (:key x) cb])))))

(defn create-client [opts]
  (let [ch (chan)
        cqrs (cqrs/create-client opts)]
    (remote-loop ch client)
    {:send-to-chan (send-to-chan ch client)}))
