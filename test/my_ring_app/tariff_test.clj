(ns my-ring-app.tariff-test
  (:require [clojure.test :refer :all]
            [my-ring-app.tariff :as tariff]
            [my-ring-app.migration :as migration]
            [my-ring-app.auth :as auth]
            [clojure.java.jdbc :as jdbc]
            [my-ring-app.config :refer [db-spec]]))

(defn setup-db [f]
  (migration/run-migrations!)
  (auth/init-db!)
  (tariff/load-plans!)
  (f))

(use-fixtures :once setup-db)

(deftest test-plans-loaded
  (testing "Тарифные планы загружены из кэша"
    (let [plans (tariff/get-all-plans)]
      (is (seq plans) "Должны быть загружены тарифные планы")
      (is (= 3 (count plans)) "Должно быть 3 тарифных плана"))))

(deftest test-get-plan-by-code
  (testing "Получение плана по коду")
  (let [free (tariff/get-plan :free)
        pro (tariff/get-plan :pro)
        enterprise (tariff/get-plan :enterprise)]
    (is (= "free" (:code free)))
    (is (= "Бесплатный" (:name free)))
    (is (= 10 (:max-workers free)))
    (is (= 1 (:max-orgs free)))
    (is (= 0 (:price-monthly free)))
    (is (= "Pro" (:name pro)))
    (is (= 50 (:max-workers pro)))
    (is (= "Enterprise" (:name enterprise)))
    (is (= 999999 (:max-workers enterprise)))))

(deftest test-get-plan-by-id
  (testing "Получение плана по ID")
  (let [plan1 (tariff/get-plan 1)
        plan2 (tariff/get-plan 2)
        plan3 (tariff/get-plan 3)]
    (is (= "free" (:code plan1)))
    (is (= "pro" (:code plan2)))
    (is (= "enterprise" (:code plan3)))))

(deftest test-get-plan-unknown
  (testing "Неизвестный план возвращает nil")
  (is (nil? (tariff/get-plan :unknown)))
  (is (nil? (tariff/get-plan 999))))

(deftest test-plan-features
  (testing "Проверка features для разных планов")
  (let [free (tariff/get-plan :free)
        pro (tariff/get-plan :pro)
        ent (tariff/get-plan :enterprise)]
    (is (false? (get-in free [:features :export])))
    (is (false? (get-in free [:features :reports])))
    (is (false? (get-in free [:features :api])))
    (is (true? (get-in pro [:features :export])))
    (is (true? (get-in pro [:features :reports])))
    (is (true? (get-in pro [:features :email])))
    (is (true? (get-in ent [:features :api])))
    (is (true? (get-in ent [:features :1c])))))

(deftest test-check-worker-limit-free
  (testing "Free план: лимит 10 работников")
  (let [result (tariff/check-worker-limit 1)]
    (is (map? result))
    (is (contains? result :allowed))
    (is (contains? result :current))
    (is (contains? result :limit))
    (is (= 10 (:limit result)))))

(deftest test-check-feature-free
  (testing "Free план: export недоступен"
    (let [result (tariff/check-feature 1 :export)]
      (is (false? (:allowed result)))
      (is (string? (:message result))))))

(deftest test-check-feature-pro
  (testing "Pro план: export доступен после смены плана"
    (tariff/update-org-plan! 1 2 1)
    (let [result (tariff/check-feature 1 :export)]
      (is (true? (:allowed result))))
    (tariff/update-org-plan! 1 1 1)))

(deftest test-check-feature-enterprise
  (testing "Enterprise план: все функции доступны"
    (tariff/update-org-plan! 1 3 1)
    (let [result (tariff/check-feature 1 :api)]
      (is (true? (:allowed result))))
    (let [result (tariff/check-feature 1 :1c)]
      (is (true? (:allowed result))))
    (tariff/update-org-plan! 1 1 1)))

(deftest test-get-org-plan-default
  (testing "Организация 1 имеет Free план по умолчанию")
  (let [plan (tariff/get-org-plan 1)]
    (is (not (nil? plan)))
    (is (= "free" (:code plan)))))

(deftest test-update-org-plan
  (testing "Обновление тарифного плана организации")
  (let [result (tariff/update-org-plan! 1 2 1)]
    (is (:success result))
    (is (:message result))
    (let [plan (tariff/get-org-plan 1)]
      (is (= "pro" (:code plan))))
    (tariff/update-org-plan! 1 1 1)))

(deftest test-update-org-plan-invalid
  (testing "Обновление на несуществующий план")
  (let [result (tariff/update-org-plan! 1 999 1)]
    (is (false? (:success result)))))

(deftest test-worker-count
  (testing "Подсчёт работников организации")
  (let [count (tariff/get-worker-count 1)]
    (is (number? count))
    (is (>= count 0))))

(deftest test-refresh-cache
  (testing "Перезагрузка планов из БД")
  (let [plans (tariff/load-plans!)]
    (is (seq plans))
    (is (some #(= "free" (:code %)) plans))
    (is (some #(= "pro" (:code %)) plans))
    (is (some #(= "enterprise" (:code %)) plans))))

(deftest test-plan-prices
  (testing "Цены тарифных планов")
  (let [free (tariff/get-plan :free)
        pro (tariff/get-plan :pro)
        ent (tariff/get-plan :enterprise)]
    (is (= 0 (:price-monthly free)))
    (is (= 0 (:price-yearly free)))
    (is (= 2990 (:price-monthly pro)))
    (is (= 29900 (:price-yearly pro)))
    (is (= 9990 (:price-monthly ent)))
    (is (= 99900 (:price-yearly ent)))))
