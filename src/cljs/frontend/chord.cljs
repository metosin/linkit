(ns frontend.chord
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! put! close!]]
            [om.next :as om]))

(defonce conn (atom nil))

(defn connect [reconciler]
  (go
    (when @conn
      (close! @conn))
    (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:3000/ws" {:format :transit-json}))]
      (reset! conn ws-channel)
      (when-not error
        (loop [m (<! ws-channel)]
          (when m
            (om/transact! reconciler `[~(:message m)])
            (recur (<! ws-channel))))))))

