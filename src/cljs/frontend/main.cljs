(ns frontend.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! chan put!]]
            [sablono.core :as html :refer-macros [html]]
            [kekkonen.client.cqrs :as cqrs]
            [potpuri.core :as potpuri]
            [om.next :as om :refer-macros [defui]]
            cljsjs.react.dom))

(enable-console-print!)

(def client (cqrs/create-client {:base-uri ""}))

(def app-state (atom {:links/by-id {}
                      :links/all []}))

(defui Link
  static om/Ident
  (ident [this {:keys [_id]}]
    [:links/by-id _id])
  static om/IQuery
  (query [this]
    '[:_id :title :url :favicon :likes])
  Object
  (render [this]
    (let [{:keys [title url favicon likes] :as props} (om/props this)]
      (html
        [:div.link
         likes
         [:button {:type "button"
                   :on-click (fn [e]
                               (om/transact! this `[(links/like {:link-id ~(:_id props)
                                                                 :user "juho"})]))}
          "+"]
         [:button {:type "button"
                   :on-click (fn [e]
                               (om/transact! this `[(links/dislike {:link-id ~(:_id props)
                                                                    :user "juho"})]))}
          "-"]
         [:a {:href url}
          (if favicon [:img {:src favicon :width 32 :height 32}])
          title]]))))

(def link (om/factory Link {:keyfn :_id}))

(defui Main
  static om/IQuery
  (query [this]
    (let [subquery (om/get-query Link)]
      `[{:links/all ~subquery}]))
  Object
  (render [this]
    (let [{:keys [links/all]} (om/props this)]
      (html
        [:div
         [:h1 "Hello World"]
         [:div.links
          (for [x all]
            (link x))]]))))

(defmulti read om/dispatch)

(defn get-links [state key]
  (let [st @state]
    (into [] (map #(get-in st %)) (get st key))))

(defmethod read :links/all
  [{:keys [state ast] :as env} key params]
  {:value (get-links state key)
   :query ast})

(defmulti mutate om/dispatch)

(defmethod mutate :default [_ x params]
  {:remote true})

(def query-chan (chan))
(def command-chan (chan))

(defn query-loop [c]
  (go
    (loop [[query cb] (<! c)]
      ; FIXME: Hardcoded to links/all query
      (let [{:keys [body]} (<! (cqrs/query client query))]
        (cb {:links/by-id (into {} (map (juxt :_id identity) body))
             :links/all (into [] (map (fn [{:keys [_id]}] [:links/by-id _id]) body))}))
      (recur (<! c)))))

(defn command-loop [c]
  (go
    (loop [[[command params] cb] (<! c)]
      (println command params)
      (let [{:keys [body]} (<! (cqrs/command client (keyword command) params))]
        (println body))
      (recur (<! c)))))

(defn send-to-chan [queries commands]
  (fn [{:keys [query remote] :as y} cb]
    (when query
      (let [{[x] :children} (om/query->ast query)]
        (put! queries [(:key x) cb])))
    (when remote
      (put! commands [(first remote) cb]))))

(query-loop query-chan)
(command-loop command-chan)

(def parser (om/parser {:read read :mutate mutate}))
(def reconciler (om/reconciler {:state app-state
                                :parser parser
                                :send (send-to-chan query-chan command-chan)
                                :remotes [:remote :query :command]}))

(defn init! []
  (om/add-root! reconciler Main (js/document.getElementById "app")))

(init!)
