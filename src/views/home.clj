(ns my-ring-app.views.home
  (:require [my-ring-app.views.layout :refer [wrap-html]]))

(defn render-home []
  "Рендер главной страницы"
  (wrap-html
    "<div class='alert alert-info'>Добро пожаловать в систему управления персоналом! Выберите раздел в меню выше.</div>"
    "Главная"
    "home"))