(ns frontend.main
  (:require [sablono.core :as html :refer-macros [html]]
            [kekkonen.client.cqrs :as cqrs]
            [potpuri.core :as potpuri]
            [om.core :as om]))

(enable-console-print!)

(defn main []
  (reify
    om/IRender
    (render [this]
      (html
        [:h1 "Hello World"]))))

(defn init! []
  (js/console.log "main init!")
  (om/root main {} {:target (js/document.getElementById "app")}))

(init!)
