(ns my-ring-app.views.dashboard
  (:require [my-ring-app.views.layout :as layout]))

(defn- format-currency [amount]
  "Форматирование суммы в виде валюты"
  (if (nil? amount)
    "0 ₽"
    (format "%,.2f ₽" (double amount))))

(defn- render-stats-cards [stats]
  "Рендеринг карточек со статистикой"
  (let [total-workers (:total-workers stats 0)
        total-shops (:total-shops stats 0)
        avg-salary (:avg-salary stats 0)
        total-payroll (:total-payroll stats 0)]
    (str "<div class='dashboard-stats'>"
         "<div class='stat-card stat-card--primary'>"
         "<div class='stat-card__icon'>👥</div>"
         "<div class='stat-card__content'>"
         "<div class='stat-card__value'>" total-workers "</div>"
         "<div class='stat-card__label'>Всего работников</div>"
         "</div>"
         "</div>"
         "<div class='stat-card stat-card--success'>"
         "<div class='stat-card__icon'>🏭</div>"
         "<div class='stat-card__content'>"
         "<div class='stat-card__value'>" total-shops "</div>"
         "<div class='stat-card__label'>Цехов</div>"
         "</div>"
         "</div>"
         "<div class='stat-card stat-card--info'>"
         "<div class='stat-card__icon'>💰</div>"
         "<div class='stat-card__content'>"
         "<div class='stat-card__value'>" (format-currency avg-salary) "</div>"
         "<div class='stat-card__label'>Средняя зарплата</div>"
         "</div>"
         "</div>"
         "<div class='stat-card stat-card--warning'>"
         "<div class='stat-card__icon'>📊</div>"
         "<div class='stat-card__content'>"
         "<div class='stat-card__value'>" (format-currency total-payroll) "</div>"
         "<div class='stat-card__label'>Фонд оплаты труда</div>"
         "</div>"
         "</div>"
         "</div>")))

(defn- render-chart-container [title chart-id chart-type labels data colors]
  "Рендеринг контейнера для диаграммы"
  (let [labels-json (str "[" (clojure.string/join "," (map #(str "\"" % "\"") labels)) "]")
        data-json (str "[" (clojure.string/join "," (map str data)) "]")
        colors-json (str "[" (clojure.string/join "," (map #(str "\"" % "\"") colors)) "]")]
    (str "<div class='chart-container'>"
         "<h3 class='chart-title'>" title "</h3>"
         "<canvas id='" chart-id "'></canvas>"
         "<script>"
         "new Chart(document.getElementById('" chart-id "'), {"
         "type: '" chart-type "',"
         "data: {"
         "labels: " labels-json ","
         "datasets: [{"
         "data: " data-json ","
         "backgroundColor: " colors-json ","
         "borderWidth: 2,"
         "borderColor: '#fff'"
         "}]"
         "},"
         "options: {"
         "responsive: true,"
         "maintainAspectRatio: true,"
         "plugins: {"
         "legend: {"
         "position: 'bottom',"
         "labels: { padding: 15, font: { size: 12 } }"
         "}"
         "}"
         "}"
         "});"
         "</script>"
         "</div>")))

(defn- render-bar-chart [title chart-id labels data]
  "Рендеринг столбчатой диаграммы"
  (let [colors (vec (repeat (count labels) "rgba(102, 126, 234, 0.8)"))]
    (render-chart-container title chart-id "bar" labels data colors)))

(defn- render-pie-chart [title chart-id labels data]
  "Рендеринг круговой диаграммы"
  (let [colors ["#667eea" "#764ba2" "#f093fb" "#f5576c" "#4facfe" "#00f2fe" "#43e97b" "#38f9d7"]]
    (render-chart-container title chart-id "pie" labels data (vec (take (count labels) colors)))))

(defn- render-distribution-charts [by-shop by-category by-rank]
  "Рендеринг диаграмм распределения"
  (str "<div class='charts-row'>"
       ;; По цехам
       (render-bar-chart "Распределение по цехам"
                         "shopChart"
                         (vec (map :name by-shop))
                         (vec (map :count by-shop)))
       ;; По категориям
       (render-pie-chart "Распределение по категориям"
                         "categoryChart"
                         (vec (map :name by-category))
                         (vec (map :count by-category)))
       "</div>"
       "<div class='charts-row'>"
       ;; По разрядам
       (render-bar-chart "Распределение по разрядам"
                         "rankChart"
                         (vec (map :name by-rank))
                         (vec (map :count by-rank)))
       "</div>"))

(defn- render-payroll-chart [payroll-by-month]
  "Рендеринг графика фонда оплаты труда"
  (let [labels (vec (map #(str (:год %) "-" (format "%02d" (:месяц %))) payroll-by-month))
        data (vec (map :total payroll-by-month))]
    (render-chart-container "Фонд оплаты труда по месяцам"
                            "payrollChart"
                            "line"
                            labels
                            data
                            ["rgba(76, 175, 80, 0.8)"])))

(defn- render-top-workers [top-workers]
  "Рендеринг таблицы топ работников"
  (str "<div class='dashboard-table'>"
       "<h3 class='table-title'>🏆 Топ-5 работников по зарплате</h3>"
       "<table class='data-table'>"
       "<thead>"
       "<tr>"
       "<th>ФИО</th>"
       "<th>Цех</th>"
       "<th>Зарплата</th>"
       "</tr>"
       "</thead>"
       "<tbody>"
       (apply str (for [w top-workers]
                    (str "<tr>"
                         "<td>" (layout/html-escape (str (:фамилия w) " " (:имя w) " " (:отчество w))) "</td>"
                         "<td>" (layout/html-escape (or (:цех w) "-")) "</td>"
                         "<td>" (format-currency (:max_salary w)) "</td>"
                         "</tr>")))
       "</tbody>"
       "</table>"
       "</div>"))

(defn- render-recent-hires [recent-hires]
  "Рендеринг таблицы последних принятых"
  (str "<div class='dashboard-table'>"
       "<h3 class='table-title'>📋 Последние принятые работники</h3>"
       "<table class='data-table'>"
       "<thead>"
       "<tr>"
       "<th>ФИО</th>"
       "<th>Цех</th>"
       "<th>Дата приёма</th>"
       "</tr>"
       "</thead>"
       "<tbody>"
       (apply str (for [w recent-hires]
                    (str "<tr>"
                         "<td><a href='/workers/" (:id w) "/edit' class='worker-link'>"
                         (layout/html-escape (str (:фамилия w) " " (:имя w) " " (:отчество w))) "</a></td>"
                         "<td>" (layout/html-escape (or (:цех w) "-")) "</td>"
                         "<td>" (layout/html-escape (:дата_приема w)) "</td>"
                         "</tr>")))
       "</tbody>"
       "</table>"
       "</div>"))

(defn render-dashboard-page [dashboard-data]
  "Рендеринг полной страницы дашборда"
  (let [stats (:stats dashboard-data)
        by-shop (:by-shop dashboard-data)
        by-category (:by-category dashboard-data)
        by-rank (:by-rank dashboard-data)
        payroll-by-month (:payroll-by-month dashboard-data)
        top-workers (:top-workers dashboard-data)
        recent-hires (:recent-hires dashboard-data)]
    (layout/wrap-html
     (str "<h1 style='margin-bottom: 30px;'>📊 Дашборд аналитики</h1>"
          ;; Карточки статистики
          (render-stats-cards stats)
          ;; Диаграммы распределения
          (render-distribution-charts by-shop by-category by-rank)
          ;; График ФОТ
          (render-payroll-chart payroll-by-month)
          ;; Таблицы
          "<div class='tables-row'>"
          (render-top-workers top-workers)
          (render-recent-hires recent-hires)
          "</div>")
     "Дашборд"
     "dashboard")))
