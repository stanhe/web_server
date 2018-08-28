(ns web3.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [web3.handler :refer :all]
            [cheshire.core :as json]))

(deftest test-app
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World"))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

(deftest json-response-test
  (testing "the /info endpoint"
    (let [response (app (mock/request :get "/info"))]
      (testing "return a 200"
        (is (= 200 (:status response))))
      (testing "with a valid Json body"
        (let [info (json/decode (:body response))]
          (testing "containing the expected keys"
            (is (= #{"Java Version" "OS Name" "OS Version"}
                   (set (keys info))))))))))
