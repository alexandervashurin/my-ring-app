(ns my-ring-app.views.dashboard
  (:require [my-ring-app.views.layout :as layout]))

(defn- format-currency [amount]
  "Форматирование суммы в виде валюты"
  (if (nil? amount)
    "0 ₽"
    (format "%,.2f ₽" (double amount))))

(defn- format-number [num]
  "Форматирование числа с разделителями"
  (if (nil? num)
    "0"
    (format "%,d" (int num))))

(defn- render-stats-cards [stats attendance]
  "Рендеринг карточек со статистикой"
  (let [total-workers (:total-workers stats 0)
        total-shops (:total-shops stats 0)
        avg-salary (:avg-salary stats 0)
        total-payroll (:total-payroll stats 0)
        avg-hours (int (:avg-hours attendance 0))
        avg-sick-days (:avg-sick-days attendance 0)
        avg-business-days (:avg-business-days attendance 0)]
    (str "<div class='dashboard-stats'>"
         "<div class='stat-card stat-card--primary'>"
         "<div class='stat-card__icon'>👥</div>"
         "<div class='stat-card__content'>"
         "<div class='stat-card__value'>" total-workers "</div>"
         "<div class='stat-card__label'>Всего работников</div>"
         "<div class='stat-card__trend stat-card__trend--up'>📈 Стабильно</div>"
         "</div>"
         "</div>"
         "<div class='stat-card stat-card--success'>"
         "<div class='stat-card__icon'>🏭</div>"
         "<div class='stat-card__content'>"
         "<div class='stat-card__value'>" total-shops "</div>"
         "<div class='stat-card__label'>Цехов</div>"
         "<div class='stat-card__trend'>📊 Все активны</div>"
         "</div>"
         "</div>"
         "<div class='stat-card stat-card--info'>"
         "<div class='stat-card__icon'>💰</div>"
         "<div class='stat-card__content'>"
         "<div class='stat-card__value'>" (format-currency avg-salary) "</div>"
         "<div class='stat-card__label'>Средняя зарплата</div>"
         "<div class='stat-card__trend stat-card__trend--up'>📈 +2.5%</div>"
         "</div>"
         "</div>"
         "<div class='stat-card stat-card--warning'>"
         "<div class='stat-card__icon'>📊</div>"
         "<div class='stat-card__content'>"
         "<div class='stat-card__value'>" (format-currency total-payroll) "</div>"
         "<div class='stat-card__label'>Фонд оплаты труда</div>"
         "<div class='stat-card__trend stat-card__trend--down'>📉 -1.2%</div>"
         "</div>"
         "</div>"
         "<div class='stat-card stat-card--purple'>"
         "<div class='stat-card__icon'>⏱️</div>"
         "<div class='stat-card__content'>"
         "<div class='stat-card__value'>" avg-hours "</div>"
         "<div class='stat-card__label'>Среднее кол-во часов</div>"
         "<div class='stat-card__trend'>норма: 160 ч</div>"
         "</div>"
         "</div>"
         "<div class='stat-card stat-card--blue'>"
         "<div class='stat-card__icon'>🏥</div>"
         "<div class='stat-card__content'>"
         "<div class='stat-card__value'>" (format "%.1f" avg-sick-days) "</div>"
         "<div class='stat-card__label'>Больничные дни (ср)</div>"
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

(defn- render-chart-containers
  "Рендеринг контейнеров для графиков Chart.js"
  []
  (str "<div class='dashboard-header'>"
       "<h1 style='margin-bottom: 30px;'>📊 Дашборд аналитики</h1>"
       "<div class='dashboard-actions'>"
       "<button onclick='location.reload()' class='btn btn-refresh' title='Обновить данные'>🔄 Обновить</button>"
       "</div>"
       "</div>"
       "<div class='charts-row'>"
       "<div class='chart-container'>"
       "<h3 class='chart-title'>🏭 Распределение по цехам</h3>"
       "<canvas id='chart-workers-by-shop'></canvas>"
       "</div>"
       "<div class='chart-container'>"
       "<h3 class='chart-title'>👥 Распределение по категориям</h3>"
       "<canvas id='chart-workers-by-category'></canvas>"
       "</div>"
       "</div>"
       "<div class='charts-row'>"
       "<div class='chart-container'>"
       "<h3 class='chart-title'>💰 Распределение по зарплате</h3>"
       "<canvas id='chart-salary-distribution'></canvas>"
       "</div>"
       "<div class='chart-container'>"
       "<h3 class='chart-title'>📈 Фонд оплаты по месяцам</h3>"
       "<canvas id='chart-payroll-by-month'></canvas>"
       "</div>"
       "</div>"
       "<div class='charts-row'>"
       "<div class='chart-container'>"
       "<h3 class='chart-title'>🎯 Распределение по разрядам</h3>"
       "<canvas id='chart-workers-by-rank'></canvas>"
       "</div>"
       "</div>"))

(defn- render-distribution-charts [by-shop by-category by-rank salary-distribution]
  "Рендеринг диаграмм распределения"
  (str "<div class='dashboard-header'>"
       "<h1 style='margin-bottom: 30px;'>📊 Дашборд аналитики</h1>"
       "<div class='dashboard-actions'>"
       "<button onclick='location.reload()' class='btn btn-refresh' title='Обновить данные'>🔄 Обновить</button>"
       "<button onclick='exportToCSV()' class='btn btn-export' title='Экспорт в CSV'>📥 Экспорт CSV</button>"
       "</div>"
       "</div>"
       "<div class='charts-row'>"
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
       ;; По уровню зарплаты
       (render-pie-chart "Распределение по зарплате"
                         "salaryChart"
                         (vec (map :name salary-distribution))
                         (vec (map :count salary-distribution)))
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
        recent-hires (:recent-hires dashboard-data)
        salary-distribution (:salary-distribution dashboard-data)
        attendance (:attendance dashboard-data)
        ;; Данные для JavaScript графиков
        chart-data (str "window.DashboardData = {"
                        "byShop: [" (apply str (interpose "," (for [item by-shop]
                                                               (str "{name: \"" (:name item) "\", count: " (:count item) "}"))) ) "],"
                        "byCategory: [" (apply str (interpose "," (for [item by-category]
                                                                    (str "{name: \"" (:name item) "\", count: " (:count item) "}"))) ) "],"
                        "byRank: [" (apply str (interpose "," (for [item by-rank]
                                                              (str "{name: \"" (:name item) "\", count: " (:count item) "}"))) ) "],"
                        "salaryDistribution: [" (apply str (interpose "," (for [item salary-distribution]
                                                                           (str "{name: \"" (:name item) "\", count: " (:count item) "}"))) ) "],"
                        "payrollByMonth: [" (apply str (interpose "," (for [item payroll-by-month]
                                                                        (str "{month: " (:месяц item) ", year: " (:год item) ", total: " (:total item) "}"))) ) "]"
                        "};")]
    (layout/wrap-html
     (str
      ;; Данные для графиков
      "<script>" chart-data "</script>"
      ;; Карточки статистики
      (render-stats-cards stats attendance)
      ;; Контейнеры для графиков
      (render-chart-containers)
      ;; Таблицы
      "<div class='tables-row'>"
      (render-top-workers top-workers)
      (render-recent-hires recent-hires)
      "</div>")
     "Дашборд"
     "dashboard")))
