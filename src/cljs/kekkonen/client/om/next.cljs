(ns kekkonen.client.om.next
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! chan put!]]
            [kekkonen.client.cqrs :as cqrs]
            [ring.util.http-predicates :refer [ok?]]
            [om.next :as om]
            [cljs.pprint :refer [pprint]]))

(defn remote-loop
  "Runs queries and commands serial. It's important to run queries and
  commands in order so that the side-effects done by commands are
  executed before the query triggered by transaction sees the side-effects.

  FIXME: Can be optimized by running commands serial and queries parallel
  when there are no commands queued."
  [ch client]
  (go
    (loop [[type x cb] (<! ch)]
      (case type
        :query
        (let [ast (om/query->ast x)
              {:keys [dispatch-key key]} (first (:children ast))
              params (if (seqable? key) (second key))
              ; _ (println x)
              ; _ (pprint ast)

              res (<! (cqrs/query client dispatch-key params))]
          (when (ok? res)
            ; NOTE: (cb {:links/all [...]})
            ; Results are automatically normalized
            ;
            ; NOTE: (cb {[:links/by-id ?] {...}})
            ; When key is an identity, it's automatically merged to correct path
            (cb {key (:body res)})))

        :command
        (let [[command params] (first x)]
          ; (println x)
          (<! (cqrs/command client (keyword command) params))))

      (recur (<! ch)))))

(defn send-to-chan
  "Creates Om Next send function."
  [ch client]
  (fn [{:keys [query remote] :as env} cb]
    ; If same transaction has both command and query, commands should be executed first
    (when remote
      (put! ch [:command remote cb]))
    (when query
      (put! ch [:query query cb]))))

(defn create-client [opts]
  (let [ch (chan)
        client (cqrs/create-client opts)]
    (remote-loop ch client)
    {:send (send-to-chan ch client)}))
