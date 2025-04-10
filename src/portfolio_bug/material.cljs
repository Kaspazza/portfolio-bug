(ns portfolio-bug.material
  (:require ["@mui/material/TextField" :as MuiTextField]
            [reagent.core :as r]))

(def text-field (r/adapt-react-class (.-default MuiTextField)))
