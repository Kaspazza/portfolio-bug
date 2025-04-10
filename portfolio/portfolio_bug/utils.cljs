(ns portfolio-bug.utils
  (:require ["@emotion/cache" :default createCache]
            ["@emotion/react" :refer [CacheProvider]]
            ["@mui/material/CssBaseline" :as MuiCssBaseline]
            ["@mui/material/styles" :as mui-styles]
            ["react-dom" :as react-dom]
            [camel-snake-kebab.core :refer
             [->camelCaseString ->kebab-case-keyword]]
            [react :as react]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [reagent.core :as r]
            [goog.object :as obj]))

(defn ^:private ref-key?
  [k]
  (and (string? k) (or (.endsWith k "ref") (.endsWith k "Ref"))))

(def ^:private color-key?
  #{:A100 :A200 :A400 :A700 "A100" "A200" "A400" "A700"})

(defn ^:private numeric-string?
  [s]
  (and (string? s) (some? (re-matches #"[0-9]+" s))))

(defn ^:private pascal-case?
  [s]
  (and (string? s)
       (contains? #{\A \B \C \D \E \F \G \H \I \J \K \L \M \N \O \P \Q \R \S \T
                    \U \V \W \X \Y \Z}
                  (first s))))

(defn ^:private keyword-safe? [s] (some? (re-matches #"[-*+!?<>='&$%#|\w]+" s)))

(defn ^:private key->str
  [k]
  (let [n (name k)]
    (cond (color-key? k) n
          (str/starts-with? n "data-") n
          (str/starts-with? n "aria-") n
          (pascal-case? n) n
          :else (->camelCaseString k))))

(defn ^:private js-key->clj
  [k]
  (cond (keyword? k) k
        (color-key? k) (keyword k)
        (numeric-string? k) (js/parseInt k)
        (keyword-safe? k)
          (if (pascal-case? k) (keyword k) (->kebab-case-keyword k))
        :else k))


(defn ^:private convert-map-keys
  [m f]
  (postwalk (fn [x] (if (map-entry? x) [(f (key x)) (val x)] x)) m))

(defn js->clj'
  [obj]
  (let [convert (fn convert [x]
                  (cond (seq? x) (doall (map convert x))
                        (map-entry? x)
                          (MapEntry. (convert (key x)) (convert (val x)) nil)
                        (coll? x) (into (empty x) (map convert) x)
                        (array? x) (persistent! (reduce #(conj! %1 (convert %2))
                                                  (transient [])
                                                  x))
                        (react/isValidElement x) x
                        (identical? (type x) js/Object)
                          (persistent! (reduce (fn [r k]
                                                 (assoc! r
                                                         (js-key->clj k)
                                                         (if (ref-key? k)
                                                           (obj/get x k)
                                                           (convert
                                                             (obj/get x k)))))
                                         (transient {})
                                         (js-keys x)))
                        :else x))]
    (convert obj)))

(defn clj->js'
  [obj]
  (clj->js (convert-map-keys obj (fn [k] (if (keyword? k) (key->str k) k)))))

(def ^:private theme-provider* (r/adapt-react-class mui-styles/ThemeProvider))

(defn theme-provider
  "Component that takes a theme object and makes it available in child components.
   It should preferably be used at the root of your component tree."
  [theme & children]
  (into [theme-provider* {:theme (clj->js' theme)}] children))

(defn create-theme
  "Takes an incomplete theme object and adds the missing parts"
  [options]
  (js->clj' (mui-styles/createTheme (clj->js' options))))

(def css-baseline (r/adapt-react-class (.-default MuiCssBaseline)))

(defn mui-iframe
  "Fixes material ui css problems in iframe, by copying styles to the iframe"
  [{:keys [children]}]
  (let [content-ref (r/atom nil)
        cache (r/atom nil)]
    (r/track! (fn []
                (when @content-ref
                  (reset! cache (createCache #js {:key "mui",
                                                  :container @content-ref})))))
    (fn [] [:> CacheProvider {:value @cache}
            [:div
             {:ref #(when (or (and (some? %) (nil? @content-ref))
                              (and (some? %)
                                   (some? @content-ref)
                                   (not= (.-innerHTML %)
                                         (.-innerHTML @content-ref))))
                      ;;^ condition for performance
                      (reset! content-ref %))}
             (when @content-ref
               (react-dom/createPortal children @content-ref))]])))

(defn layout
  "Our default layout"
  [& comps]
  (theme-provider
    (create-theme {})
    [css-baseline]
    (into [:span] (doall (for [cmp comps] ^{:key (str (random-uuid))} cmp)))))

(defn wrapper
  "Wrapper to use with each component. Enables easy future updates of all components in case of using libraries that require it. Currently stores our app layout and enables MUI usage"
  [& children]
  [:f> mui-iframe
   {:key (str "mui-iframe-" (random-uuid)),
    :children (r/as-element (into [layout] children))}])
