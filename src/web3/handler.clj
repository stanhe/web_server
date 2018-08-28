(ns web3.handler
  (:require [cheshire.core :as json]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults
             :refer
             [api-defaults site-defaults wrap-defaults]]
            [ring.util.response :as ring-response]))

(defn wrap-500-handler
  [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           (-> (ring-response/response (.getMessage e))
               (ring-response/status 500)
               (ring-response/content-type "text/plain")
               (ring-response/charset "utf-8")
               )))))

(defn wrap-slurp-body
  [handler]
  (fn [request]
    (if (instance? java.io.InputStream (:body request))
      (let [prepared-request (update request :body slurp)]
        (handler prepared-request))
      (handler request))))

(defn back1
  [id]
  (let [input (Integer/parseInt id)]
    (apply str "random " input " is: " (take input (repeatedly #(rand-int 10))))
    )
  )

(defn body-echo-handler
  [request]
  (if-let [body (:body request)]
    (-> (ring-response/response body)
        (ring-response/content-type "text/plain")
        (ring-response/charset "utf-8"))
    (-> (ring-response/response "You must submit a body with your request!")
        (ring-response/status 400))))

(def body-echo-app
  (-> body-echo-handler
      wrap-500-handler
      wrap-slurp-body))
;;test with (body-echo-app (mock/request :post "/" "Echo!"))

(defn wrap-json
  [handler]
  (fn [request]
    (if-let [prepd-request (try (update request :body json/decode)
                                (catch com.fasterxml.jackson.core.JsonParseException e nil))]
      (handler prepd-request)
      (-> (ring-response/response "Sorry,that's not Json.")
          (ring-response/status 400)))))

(defn handle-clojurefy
  [request]
  (-> (:body request)
      str
      ring-response/response
      (ring-response/content-type "application/edn")))

(def body-routes
  (-> (routes
       (ANY "/echo" [:as {body :body}] (str body))
       (POST "/cloj" [] (wrap-json handle-clojurefy)))
      (wrap-routes wrap-slurp-body)
      ))

(defroutes no-body-routes
  (GET "/" [] "Hello World123")
  (GET "/b/:id" [id] (back1 id))
  (GET "/error" [] (/ 1 0))
  ;;(ANY "/echo" [:as request] (body-echo-handler request))
  ;; (ANY "/echo" [:as {body :body}] (str body))
  (route/not-found "Not Found"))

(def app-routes
  (routes body-routes no-body-routes))

(def app
  (-> app-routes
      wrap-500-handler
      (wrap-defaults api-defaults)))

;; (def app
;;   (-> app-routes
;;       ;; (wrap-defaults api-defaults)
;;       (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
;;       wrap-500-handler
;;       wrap-slurp-body
;;       ))
