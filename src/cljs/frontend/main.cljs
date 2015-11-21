(ns frontend.main
  (:require [sablono.core :as html :refer-macros [html]]
            [kekkonen.client.cqrs :as cqrs]
            [potpuri.core :as potpuri]
            [om.next :as om :refer-macros [defui]]
            cljsjs.react.dom))

(enable-console-print!)

(def app-state (atom {:link/by-id {"5650b5e6f7ff9b5ff023077b" {:likes 1
                                                               :dateTime "2015-11-21T20:20:22.537+02:00"
                                                               :favicon "http://clojars.org/favicon-96x96.png?v=47K2kprJd7"
                                                               :title "Clojars"
                                                               :url "http://clojars.org"
                                                               :_id "5650b5e6f7ff9b5ff023077b"}}
                      :links/list [[:link/by-id "5650b5e6f7ff9b5ff023077b"]]}))

(defui Link
  static om/Ident
  (ident [this {:keys [_id]}]
    [:link/by-id _id])
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
      `[{:links/list ~subquery}]))
  Object
  (render [this]
    (let [{:keys [links/list]} (om/props this)]
      (html
        [:div
         [:h1 "Hello World"]
         [:div.links
          (for [x list]
            (link x))]]))))

(defmulti read om/dispatch)

(defn get-links [state key]
  (let [st @state]
    (into [] (map #(get-in st %)) (get st key))))

(defmethod read :link/list
  [{:keys [state ast] :as env} key params]
  {:value (get-links state key)})

(defmulti mutate om/dispatch)

(defmethod mutate 'link/like
  [{:keys [state]} _ {:keys [_id]}]
  {:action
   (fn []
     (swap! state update-in
            [:link/by-id _id :likes]
            inc))})

(defmethod mutate 'link/dislike
  [{:keys [state]} _ {:keys [_id]}]
  {:action
   (fn []
     (swap! state update-in
            [:link/by-id _id :likes]
            dec))})

(def parser (om/parser {:read read :mutate mutate}))
(def reconciler (om/reconciler {:state app-state
                                :parser parser}))

(defn init! []
  (js/console.log "main init!")
  (om/add-root! reconciler Main (js/document.getElementById "app")))

(init!)
