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
                               (om/transact! this `[(link/like ~props)]))}
          "+"]
         [:button {:type "button"
                   :on-click (fn [e]
                               (om/transact! this `[(link/dislike ~props)]))}
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

(defmethod mutate 'link/like
  [{:keys [state]} _ {:keys [_id]}]
  {:action
   (fn []
     (swap! state update-in
            [:links/by-id _id :likes]
            inc))})

(defmethod mutate 'link/dislike
  [{:keys [state]} _ {:keys [_id]}]
  {:action
   (fn []
     (swap! state update-in
            [:links/by-id _id :likes]
            dec))})

(defn search-loop [c]
  (go
    (loop [[query cb] (<! c)]
      (println query)
      (let [{:keys [body]} (<! (cqrs/query client query))]
        (println body)
        (cb {:links/by-id (into {} (map (juxt :_id identity) body))
             :links/all (into [] (map (fn [{:keys [_id]}] [:links/by-id _id]) body))}))
      (recur (<! c)))))

(defn send-to-chan [c]
  (fn [{:keys [query]} cb]
    (when query
      (let [{[x] :children} (om/query->ast query)]
        (put! c [(:key x) cb])))))

(def send-chan (chan))

(search-loop send-chan)

(def parser (om/parser {:read read :mutate mutate}))
(def reconciler (om/reconciler {:state app-state
                                :parser parser
                                :send (send-to-chan send-chan)
                                :remotes [:remote :query]}))

(defn init! []
  (js/console.log "main init!")
  (om/add-root! reconciler Main (js/document.getElementById "app")))

(init!)
