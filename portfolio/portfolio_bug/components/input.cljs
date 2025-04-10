(ns portfolio-bug.components.input
  (:require [portfolio.reagent-18 :refer-macros [defscene configure-scenes]]
            [portfolio-bug.material :as mui]
            [portfolio-bug.utils :as rpu]))

(configure-scenes {:collection :components})

(defscene input-text-empty
          [_]
          [rpu/wrapper [mui/text-field {:label "Label", :size "small"}]])

(defscene input-text-value-filled
          [_]
          [rpu/wrapper
           [mui/text-field
            {:label "Label", :value "Some text...", :size "small"}]])

(defscene
  input-text-full-width
  [_]
  [rpu/wrapper
   [mui/text-field
    {:label "Label", :value "Some text...", :size "small", :full-width true}]])

(defscene input-text-helper-text
          [_]
          [rpu/wrapper
           [mui/text-field
            {:value "Bagiete chce",
             :helperText "Helper text",
             :label "User Text",
             :size "small"}]])

(defscene multi-line-input-text
          [_]
          [rpu/wrapper
           [mui/text-field
            {:value "Bagiete chce\nBardzo...\nTeraz!",
             :label "User Text",
             :size "small",
             :sx {:min-height "100px"},
             :rows 4,
             :full-width true,
             :multiline true}]])
