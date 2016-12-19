(ns pmmt.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [pmmt.handler :refer :all]))

(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response))))))

(deftest service-routes
  (testing "/register"
    (let [user-registration {:id "efraim"
                             :pass "123456789"
                             :pass-confirm "123456789"}
          response ((app) (request :post "/register" user-registration))]
      (is (= 200 response)))))
