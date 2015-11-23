(ns frontend.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! chan put!]]
            [sablono.core :as html :refer-macros [html]]
            [kekkonen.client.cqrs :as cqrs]
            [potpuri.core :as potpuri]
            [om.next :as om :refer-macros [defui]]
            [ring.util.http-predicates :refer [ok?]]
            cljsjs.react.dom))

(enable-console-print!)

(def client (cqrs/create-client {:base-uri ""}))

(def app-state (atom {:user/name (goog.object/get js/localStorage "name")
                      :links/by-id {}
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
    (let [{:keys [_id title url favicon likes] :as props} (om/props this)
          {:keys [user/name]} (om/shared this)]
      (html
        [:div.link
         likes
         (if name
           [:button {:type "button"
                     :on-click (fn [e]
                                 (om/transact! this `[(links/like {:link-id ~_id
                                                                   :user ~name})
                                                      [:links/by-id ~_id]]))}
            "+"])
         (if name
           [:button {:type "button"
                     :on-click (fn [e]
                                 (om/transact! this `[(links/dislike {:link-id ~_id
                                                                      :user ~name})
                                                      [:links/by-id ~_id]]))}
            "-"])
         [:a {:href url}
          (if favicon [:img {:src favicon :width 32 :height 32}])
          title]]))))

(def link (om/factory Link {:keyfn :_id}))

(defui Login
  static om/IQuery
  (query [this]
    nil)

  Object
  (render [this]
    (html
      (let [{:keys [name]} (om/get-state this)]
        [:form
         {:on-submit (fn [e]
                       (.preventDefault e)
                       (.stopPropagation e)
                       (om/transact! this `[(user/set {:name ~name})
                                            :user/name]))}
         [:h1 "Please identify yourself to transact with the system:"]
         [:input {:type "text"
                  :value name
                  :on-change (fn [e]
                               (om/update-state! this assoc :name (.. e -target -value)))}]
         [:button {:type "submit"}
          "Identify"]]))))

(def login (om/factory Login))

(defui Main
  static om/IQuery
  (query [this]
    (let [subquery (om/get-query Link)]
      `[:user/name {:links/all ~subquery}]))
  Object
  (render [this]
    (let [{:keys [user/name links/all]} (om/props this)]
      (println "all" all)
      (html
        [:div
         (if name
           [:h1 "Hello " name
            [:button
             {:type "button"
              :on-click #(om/transact! this `[(user/reset)
                                              :user/name])}
             "Logout"]]
           (login))
         [:div.links
          (for [x all]
            (link x))]]))))

(defmulti read om/dispatch)

(defn get-links [state key]
  (let [st @state]
    (into [] (map #(get-in st %)) (get st key))))

(defmethod read :default [{:keys [state]} key _]
  {:value (if (seqable? key)
            (get-in @state key)
            (get @state key))})

(defmethod read :links/all
  [{:keys [state ast] :as env} key params]
  {:value (get-links state key)
   :query ast})

(defmethod read :links/by-id
  [{:keys [state ast] :as env} key params]
  (println ast)
  {:value (get-in @state (:key ast))
   :query ast})

(defmulti mutate om/dispatch)

(defmethod mutate :default [_ x params]
  {:remote true})

(defmethod mutate 'user/set [{:keys [state]} key params]
  {:action (fn []
             (goog.object/set js/localStorage "name" (:name params))
             (swap! state assoc :user/name (:name params)))})

(defmethod mutate 'user/reset [{:keys [state]} key _]
  {:action (fn []
             (goog.object/remove js/localStorage "name")
             (swap! state assoc :user/name nil))})

(def remote-chan (chan))

(defn remote-loop [c]
  (go
    (loop [[type x cb] (<! c)]
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

      (recur (<! c)))))

(defn send-to-chan [remote-chan]
  (fn [{:keys [query remote state] :as env} cb]
    ; If same transaction has both command and query, commands should be executed first
    (when remote
      (put! remote-chan [:command (first remote) cb]))
    (when query
      (let [{[x] :children} (om/query->ast query)]
        (put! remote-chan [:query (:key x) cb])))))

(remote-loop remote-chan)

(def parser (om/parser {:read read :mutate mutate}))
(def reconciler (om/reconciler {:state app-state
                                :parser parser
                                :send (send-to-chan remote-chan)
                                :shared-fn (fn [data]
                                             {:user/name (:user/name data)})
                                :remotes [:remote :query :command]}))

(defn init! []
  (om/add-root! reconciler Main (js/document.getElementById "app")))

(init!)
