(ns user
  (:require [reloaded.repl :refer [system go reset start stop]]
            [palikka.core :refer [create-context]]))

; Availble in editor
; Repl is usually in boot.user ns

(defn ctx []
  (create-context system))
