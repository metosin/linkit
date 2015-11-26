(set-env!
  :source-paths #{"src/cljs" "src/less" "test/clj" "test/cljc"}
  :resource-paths #{"src/clj" "src/cljc" "resources"}
  :dependencies '[[org.clojure/clojure    "1.7.0"]
                  [org.clojure/clojurescript "1.7.170"]
                  [org.clojure/core.async "0.2.371"]
                  [org.clojure/tools.nrepl "0.2.12"]
                  [org.clojure/tools.namespace "0.2.11"]

                  [adzerk/boot-cljs       "1.7.170-3"  :scope "test"]
                  [adzerk/boot-cljs-repl  "0.3.0"      :scope "test"]
                  [com.cemerick/piggieback "0.2.1"     :scope "test"]
                  [weasel                 "0.7.0"      :scope "test"]
                  [adzerk/boot-reload     "0.4.2"      :scope "test"]
                  [deraen/boot-less       "0.4.2"      :scope "test"]
                  [deraen/boot-ctn        "0.1.0"      :scope "test"]

                  [ring/ring-core "1.4.0"]
                  [ring/ring-devel "1.4.0"]
                  [ring/ring-defaults "0.1.5"]
                  [http-kit "2.1.19"]

                  [prismatic/schema "1.0.3"]
                  [prismatic/plumbing "0.5.2"]

                  [metosin/potpuri "0.2.3"]
                  [metosin/schema-tools "0.6.2"]
                  [metosin/palikka "0.3.0"]
                  [metosin/maailma "0.2.0"]
                  [metosin/ring-http-response "0.6.5"]
                  [metosin/ring-swagger-ui "2.1.3-2"]
                  [metosin/kekkonen "0.1.1"]
                  [metosin/lokit "0.1.0"]

                  [hiccup "1.0.5"]
                  [clj-http "2.0.0"]
                  [enlive "1.1.6"]
                  [com.novemberain/monger "3.0.1"]

                  [cljs-http "0.1.37"]
                  [org.omcljs/om "1.0.0-alpha24"]
                  [sablono "0.4.0"]

                  [org.webjars.bower/font-awesome "4.4.0"]

                  ; Workflow
                  [reloaded.repl "0.2.1"]])

(require
  '[adzerk.boot-cljs      :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl repl-env]]
  '[adzerk.boot-reload    :refer [reload]]
  '[deraen.boot-less      :refer [less]]
  '[deraen.boot-ctn       :refer [init-ctn!]]
  '[backend.main]
  '[reloaded.repl         :refer [go reset start stop system]]
  'user)

; Watch boot temp dirs
(init-ctn!)

(task-options!
  pom {:project 'application
       :version "0.1.0-SNAPSHOT"}
  aot {:namespace #{'backend.main 'com.stuartsierra.component 'com.stuartsierra.dependency}}
  jar {:main 'backend.main}
  cljs {:source-map true}
  less {:source-map true})

(deftask start-app
  [p port   PORT int  "Port"]
  (let [x (atom nil)]
    (with-post-wrap fileset
      (swap! x (fn [x]
                 (if x
                   x
                   (do (backend.main/setup-app! {:port port})
                       (go)))))
      fileset)))

(deftask dev
  "Start the dev env..."
  [p port       PORT int  "Port for web server"]
  (comp
    (watch)
    (reload)
    (less)
    (cljs-repl)
    (cljs :optimizations :none)
    (start-app :port port)))

(deftask package
  "Build the package"
  []
  (comp
    (less :compression true)
    (cljs :optimizations :advanced)
    (pom)
    ; https://github.com/boot-clj/boot/issues/278 - Uberjar by default doesn't include dependency pom files
    (uber :exclude #{#"(?i)^META-INF/[^/]*\.(MF|SF|RSA|DSA)$"})
    (aot)
    (jar :file "application.jar")))
