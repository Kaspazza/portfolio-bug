(ns portfolio-bug.portfolio
  (:require [portfolio.data :as data]
            [portfolio-bug.components.input]
            [portfolio.ui :as ui]))


(data/register-collection! :components {:title "Components"})

(defonce app (ui/start!))

(defn init [] app)
