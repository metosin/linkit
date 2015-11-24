(ns frontend.main
  (:require [sablono.core :as html :refer-macros [html]]
            [kekkonen.client.om.next :as kom]
            [om.next :as om :refer-macros [defui]]
            cljsjs.react.dom))

(enable-console-print!)

(def app-state (atom {:user/name (goog.object/get js/localStorage "name")
                      :links/by-id {}
                      :links/all []}))

(defui Link
  static om/Ident
  (ident [this {:keys [_id]}]
    [:links/by-id _id])
  static om/IQuery
  (query [this]
    '[:_id :title :url :favicon :likes :likeUsers])
  Object
  (render [this]
    (let [{:keys [_id title url favicon likes likeUsers] :as props} (om/props this)
          {:keys [user/name]} (om/shared this)
          liked? (contains? (set likeUsers) name)]
      (html
        [:div.link
         [:span.likes likes]
         (if name
           [:div.buttons
            [:button
             {:type "button"
              :disabled liked?
              :on-click (fn [e]
                          (om/transact! this `[(links/like {:link-id ~_id
                                                            :user ~name})
                                               [:links/by-id ~_id]]))}
             "+"]
            [:button
             {:type "button"
              :disabled (not liked?)
              :on-click (fn [e]
                          (om/transact! this `[(links/dislike {:link-id ~_id
                                                               :user ~name})
                                               [:links/by-id ~_id]]))}
             "-"]])
         (if favicon
           [:img {:src favicon :width 32 :height 32}]
           [:div.favicon-placeholder])
         [:a {:href url :target "new"}
          [:span.title title]
          [:span.href "(" url ")"]]]))))

(def link (om/factory Link {:keyfn :_id}))

(defui Login
  static om/IQuery
  (query [this]
    [:user/name])

  Object
  (render [this]
    (html
      (let [{:keys [user/name]} (om/props this)]
        (if name
          [:h1 "Hello " name
           [:button.pull-right
            {:type "button"
             :on-click #(om/transact! this `[(user/reset)
                                             :user/name])}
            "Logout"]]
          [:div
           {:style {:margin-bottom "40px"}}
           [:h1 "Please identify yourself to transact with the system:"]
           [:form
            {:on-submit (fn [e]
                          (.preventDefault e)
                          (.stopPropagation e)
                          (om/transact! this `[(user/set {:name ~(:name (om/get-state this))})
                                               :user/name]))}
            [:input {:type "text"
                     :value (:name (om/get-state this))
                     :on-change (fn [e]
                                  (om/update-state! this assoc :name (.. e -target -value)))}]
            [:button {:type "submit"}
             "Identify"]]])))))

(def login (om/factory Login))

(defui NewLink
  static om/IQuery
  (query [this]
         nil)

  Object
  (render [this]
    (let [{:keys [url]} (om/get-state this)]
      (html
        [:form
         {:style {:margin-top "40px" :margin-bottom "40px"}
          :on-submit (fn [e]
                       (.preventDefault e)
                       (.stopPropagation e)
                       (om/transact! this `[(links/add {:url ~url})
                                            :links/all])
                       (om/update-state! this assoc :url nil))}
         [:input {:type "text"
                  :value (or url "http://")
                  :on-change (fn [e]
                               (om/update-state! this assoc :url (.. e -target -value)))}]
         [:button {:type "submit"}
          "Add link"]]))))

(def new-link (om/factory NewLink))

(defui LinkList
  static om/IQuery
  (query [this]
    (let [subquery (om/get-query Link)]
      `[{:links/all ~subquery}]))
  Object
  (render [this]
    (let [{:keys [links/all]} (om/props this)]
      (html
        [:div.links
         (for [x all]
           (link x))]))))

(def link-list (om/factory LinkList))

(defui Main
  static om/IQuery
  (query [this]
    (vec (concat (om/get-query Login) (om/get-query LinkList))))
  Object
  (render [this]
    (let [{:keys [user/name links/all]} (om/props this)]
      (html
        [:div
         (login {:user/name name})
         (link-list {:links/all all})
         (new-link)]))))

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

(def client (kom/create-client {:base-uri "/"}))

(def parser (om/parser {:read read :mutate mutate}))
(def reconciler (om/reconciler {:state app-state
                                :parser parser
                                :send (:send-to-chan client)
                                :shared-fn (fn [data]
                                             {:user/name (:user/name data)})
                                :remotes [:remote :query :command]}))

(defn init! []
  (om/add-root! reconciler Main (js/document.getElementById "app")))

(init!)
