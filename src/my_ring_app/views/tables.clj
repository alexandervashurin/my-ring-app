(ns my-ring-app.views.tables
  (:require [my-ring-app.views.layout :refer [wrap-html html-escape]]))

(defn render-all-tables-page
  "Рендер страницы со всеми таблицами БД"
  [tables-data]
  (let [content (apply str 
                (map (fn [tbl]
                       (str "<h3>" (html-escape (:table tbl)) "</h3>"
                            "<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse; margin: 10px 0; width: 100%;'>"
                            "<thead style='background: #667eea; color: white;'>"
                            "<tr>" 
                            (apply str 
                              (map (fn [col] 
                                     (str "<th style='padding: 10px;'>" (html-escape col) "</th>")) 
                                   (keys (first (:rows tbl)))))
                            "</tr>"
                            "</thead>"
                            "<tbody>"
                            (apply str 
                              (map (fn [row]
                                     (str "<tr>"
                                          (apply str 
                                            (map (fn [[k v]] 
                                                   (str "<td style='padding: 8px; border: 1px solid #ddd;'>" 
                                                        (if (nil? v) "NULL" (html-escape (str v))) 
                                                        "</td>"))
                                                 row))
                                          "</tr>"))
                                    (:rows tbl)))
                            "</tbody>"
                            "</table>"))
                     tables-data))]
    (wrap-html content "Все таблицы" "db")))