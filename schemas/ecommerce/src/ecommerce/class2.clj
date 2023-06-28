(ns ecommerce.class2
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.db :as db]
            [ecommerce.model :as model]
            [schema.core :as s]))

(s/set-fn-validation! true)

(db/delete-database!)
(def conn (db/open-connection!))
(db/create-schema! conn)

(db/create-sample-data conn)

(pprint (db/search-all-categories (d/db conn)))
(pprint (db/search-all-products (d/db conn)))

(def checkers
  {:product/name "Checkers"
   :product/slug "/checkers"
   :product/price 15.5M
   :product/id (model/uuid)})
(db/upsert-products! conn [checkers])
(pprint (db/search-one-product (d/db conn) (:product/id checkers)))

; upsert, since product/id is unique and this id already exists
(db/upsert-products! conn [(assoc checkers :product/slug "/checkers-game")])
(pprint (db/search-one-product (d/db conn) (:product/id checkers)))

; upsert is changing both PRICE AND SLUG again to /checkers
(db/upsert-products! conn [(assoc checkers :product/price 150.5M)])
(pprint (db/search-one-product (d/db conn) (:product/id checkers)))

(defn bad-update-price []
  (println "updating price")
  (let [product (db/search-one-product (d/db conn) (:product/id checkers))
        product (assoc product :product/price 999M)]
    (db/upsert-products! conn [product])
    (println "price updated")
    product))

(defn bad-update-slug []
  (println "updating slug")
  (let [product (db/search-one-product (d/db conn) (:product/id checkers))]
    (Thread/sleep 3000)
    (let [product (assoc product :product/slug "/checkers-game-expensive")]
      (db/upsert-products! conn [product])
      (println "slug updated")
      product)))

(defn run-transactions
  [transactions]
  (let [futures (mapv #(future (%)) transactions)]
    (pprint (map deref futures))
    (pprint "final result")
    (pprint (db/search-one-product (d/db conn) (:product/id checkers)))))

; non-deterministic when we run on threads
; simultaneously upserts will usually override themselves, then we dont know what the final result will be
;(run-transactions [bad-update-price, bad-update-slug])

(defn update-price []
  (println "updating price")
  (let [product {:product/id (:product/id checkers),
                 :product/price 999M}]
    (db/upsert-products! conn [product])
    (println "price updated")
    product))

(defn update-slug []
  (println "updating slug")
  (let [product {:product/id (:product/id checkers),
                 :product/slug "/checkers-game-expensive"}]
    (Thread/sleep 3000)
    (db/upsert-products! conn [product])
    (println "slug updated")
    product))

(run-transactions [update-price, update-slug])

(defn update-slug-and-price []
  (println "updating price and slug")
  (let [product {:product/id (:product/id checkers),
                 :product/slug "/updated-checkers",
                 :product/price 555555M}]
    (Thread/sleep 3000)
    (db/upsert-products! conn [product])
    (println "price and slug updated")
    product))

; disadvantages:
; the one that is calling the function upsert knows how the function works a little bit
; to understand what it needs to pass as product parameter
; if you dont have the knowledge you can be the cause to fails
; also, it is a generic function in terms of products, maybe you dont want to
; give these powers

(run-transactions [update-price, update-slug, update-slug-and-price])