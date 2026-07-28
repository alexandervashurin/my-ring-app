(ns my-ring-app.i18n-test
  "Тесты для модуля интернационализации"
  (:require [clojure.test :refer :all]
            [my-ring-app.i18n :as i18n]))

(deftest test-t-basic
  (testing "Базовый перевод на русский"
    (let [result (i18n/t :ru :common :title)]
      (is (string? result))
      (is (not (empty? result))))))

(deftest test-t-english
  (testing "Перевод на английский"
    (let [result (i18n/t :en :common :title)]
      (is (string? result))
      (is (not (empty? result))))))

(deftest test-t-fallback
  (testing "Fallback на русский при отсутствии ключа"
    (let [result (i18n/t :en :nonexistent :key)]
      (is (string? result)))))

(deftest test-t-keyword-vs-string
  (testing "Работа с ключами как строками и ключевыми словами"
    (let [by-keyword (i18n/t :ru :common :title)
          by-string (i18n/t "ru" "common" "title")]
      (is (= by-keyword by-string)))))

(deftest test-get-language-name
  (testing "Названия языков"
    (is (= "Русский" (i18n/get-language-name :ru)))
    (is (= "English" (i18n/get-language-name :en)))
    (is (= "de" (i18n/get-language-name :de)))))

(deftest test-get-current-lang
  (testing "Получение языка из сессии"
    (is (= "ru" (i18n/get-current-lang {})))
    (is (= "en" (i18n/get-current-lang {:session {:lang "en"}})))
    (is (= "ru" (i18n/get-current-lang {:session {:lang "ru"}})))))

(deftest test-translate-field
  (testing "Перевод названия поля"
    (let [result (i18n/translate-field :ru :name)]
      (is (string? result)))))

(deftest test-translate-nav
  (testing "Перевод навигации"
    (let [result (i18n/translate-nav :ru :home)]
      (is (string? result)))))

(deftest test-translate-error
  (testing "Перевод ошибки"
    (let [result (i18n/translate-error :ru :not-found)]
      (is (string? result)))))

(deftest test-translate-message
  (testing "Перевод сообщения"
    (let [result (i18n/translate-message :ru :success)]
      (is (string? result)))))

(deftest test-get-available-languages
  (testing "Список доступных языков"
    (let [langs (i18n/get-available-languages)]
      (is (seq langs))
      (is (contains? (set langs) "ru")))))
