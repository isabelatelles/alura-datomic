(ns ecommerce.db.product
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema.core :as s]
            [ecommerce.model :as model]
            [ecommerce.db.entity :as db.entity]
            [clojure.set :as cset]))

(def rules
  '[
    [(stock ?product ?stock)
     [?product :product/stock ?stock]]
    [(stock ?product ?stock)
     [?product :product/digital true]
     [(ground ##Inf) ?stock]]                               ; could have defined 100 instead of ##Inf but truth is stock is infinity for digital products
    [(salable? ?product)
     (stock ?product ?stock)
     [(> ?stock 0)]]
    [(product-in-category ?product ?category-name)
     [?category :category/name ?category-name]
     [?product :product/category ?category]]])

(s/defn upsert!
  ([connection
    products :- [model/Product]]
   (d/transact connection products))
  ([connection
    products :- [model/Product]
    ip :- s/Str]
   (let [db-add-ip [:db/add "datomic.tx" :tx-data/ip ip]]
     (d/transact connection (conj products db-add-ip)))))

(defn upsert-without-model!
  [connection
   products]
  (d/transact connection products))

(s/defn search-all :- [model/Product]
  [db]
  (db.entity/datomic-to-entity
    (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
           :where [?product :product/id]] db)))

(s/defn search-one :- (s/maybe model/Product)
  [db
   product-id :- java.util.UUID]
  (let [result (d/pull db '[* {:product/category [*]}] [:product/id product-id])
        product (db.entity/datomic-to-entity result)]
    (if (:product/id product)
      product
      nil)))


(s/defn search-one! :- model/Product
  [db
   product-id :- java.util.UUID]
  (let [product (search-one db product-id)]
    (if product
      product
      (throw (ex-info "Did not find an entity."
                      {:type :errors/not-found,
                       :id product-id})))))

; https://docs.datomic.com/cloud/query/query-data-reference.html#q
(s/defn search-most-expensive :- model/Product
  [db]
  (db.entity/datomic-to-entity
    (first (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
                  :where [(q '[:find (max ?price)
                               :where [_ :product/price ?price]]
                             $) [[?price]]]
                  [?product :product/price ?price]]
                db))))

(s/defn search-cheapest :- model/Product
  [db]
  (db.entity/datomic-to-entity
    (first (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
                  :where [(q '[:find (min ?price)
                               :where [_ :product/price ?price]]
                             $) [[?price]]]
                  [?product :product/price ?price]]
                db))))

(s/defn search-most-expensive-price :- BigDecimal
  [db]
  (d/q '[:find (max ?price) .
         :where [_ :product/price ?price]]
       db))

(defn search-most-expensive-price-than
  [db min-price]
  (d/q '[:find ?price
         :in $ ?min
         :where [_ :product/price ?price]
                [(>= ?price ?min)]]
       db min-price))

(defn search-by-price
  [db price]
  (db.entity/datomic-to-entity
    (d/q '[:find (pull ?product [*])
           :in $ ?search-price
           :where [?product :product/price ?search-price]]
         db price)))

(defn search-by-price-and-name
  [db price name]
  (db.entity/datomic-to-entity
    (d/q '[:find (pull ?product [*])
           :in $ ?search-price ?substring
           :where [?product :product/price ?search-price]   ; important to be the first one in this order
                  [?product :product/name ?search-name]
                  [(.contains ?search-name ?substring)]]
         db price name)))

; pull and forward navigation
; searches category, then products, then category again in pull
(s/defn search-all-by-category :- [model/Product]
  [db, category :- s/Str]
  (db.entity/datomic-to-entity
    (d/q '[:find [(pull ?product [:product/id :product/name :product/slug {:product/category [*]}]) ...] ; find spec to return collection https://docs.datomic.com/pro/query/query.html#find-specifications
           :in $ % ?category-name
           :where (product-in-category ?product ?category-name)]
         db rules category)))

; pull and backward navigation
(s/defn search-all-by-category-backward
  [db, category :- s/Str]
  (db.entity/datomic-to-entity
    (d/q '[:find (pull ?category [* {:product/_category [:product/id :product/name :product/slug]}]) .
           :in $ % ?category-name
           :where [?category :category/name ?category-name]]
         db rules category)))

; https://docs.datomic.com/pro/query/query.html#bindings
(s/defn search-all-by-categories :- [model/Product]
  [db, categories :- [s/Str]]
  (db.entity/datomic-to-entity
    (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
           :in $ % [?category-name ...]                       ; collection of category names
           :where (product-in-category ?product ?category-name)]
         db, rules, categories)))


(s/defn search-all-by-categories-and-digital :- [model/Product]
  [db, categories :- [s/Str], digital? :- s/Bool]
  (db.entity/datomic-to-entity
    (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
           :in $ % [?category-name ...] ?is-digital?
           :where (product-in-category ?product ?category-name)
                  [?product :product/digital ?is-digital?]]
         db, rules, categories, digital?)))

; compare and swap: https://docs.datomic.com/cloud/transactions/transaction-functions.html#db-cas
; transactor functions are executed one by one
; code executed in transactor is a code that is blocking other transactions
; in an environment with a lot of writes, this will pile up
(s/defn update-price!
  [conn
   product-id :- java.util.UUID
   old-price :- BigDecimal
   new-price :- BigDecimal]
  (d/transact conn [[:db/cas [:product/id product-id] :product/price old-price new-price]]))

; sometimes we want to update fields with compare and swap so we dont have a situation in which
; we think we add +11 but instead we add +1, example:
; price: 20M
; new price: 30M (+10)
; new price: 31M (+1 but should be +11)

(defn- build-db-cas-transactions
  [attributes product-id old-product new-product]
  (map (fn [attribute]
         [:db/cas
          [:product/id product-id]
          attribute
          (get old-product attribute)
          (get new-product attribute)])
       attributes))

(s/defn update!
  [conn
   old-product :- model/Product
   new-product :- model/Product]
  (let [product-id (:product/id old-product)
        attributes (cset/intersection (set (keys old-product)) (set (keys new-product)))
        attributes (disj attributes :product/id)
        transactions (build-db-cas-transactions attributes product-id old-product new-product)]
    (d/transact conn transactions)))

; we can put a temp id instead of doing [ :variation/id uuid ] and defining uuid in a let
(s/defn add-variation!
  [conn
   product-id :- java.util.UUID
   variation-name :- s/Str
   variation-price :- BigDecimal]
  (d/transact conn [{ :db/id "temporary-variation"
                      :variation/name variation-name
                      :variation/price variation-price
                      :variation/id (model/uuid) }
                    { :product/id product-id
                      :product/variation "temporary-variation" }]))

(defn total-products
  [db]
  (d/q '[:find [(count ?product)]
         :where [?product :product/id]]
       db))

(s/defn remove!
  [conn product-id :- java.util.UUID]
  (d/transact conn [[:db/retractEntity [:product/id product-id]]]))

;danger because it does not have atomicity!!
;(defn- visualizations
;  [db product-id]
;  (or (d/q '[:find ?visualizations .
;             :in $ ?id
;             :where [?product :product/id ?id]
;             [?product :product/visualizations ?visualizations]]
;           db product-id)
;      0))
;
;(defn visualization!
;  [conn product-id]
;  (let [vis-until-now (visualizations (d/db conn) product-id)
;        new-value (inc vis-until-now)]
;    (d/transact conn [{:product/id product-id
;                       :product/visualizations new-value}])))

(s/defn visualization!
  [conn product-id :- java.util.UUID]
  (d/transact conn [[:increment-visualization product-id]]))

; https://docs.datomic.com/cloud/query/query-data-reference.html#aggregates-returning-a-single-value
(defn summary-of-products
  [db]
  (d/q '[:find (min ?price) (max ?price) (count ?price)
         :keys min-price max-price count-price
         :with ?product                                     ; without this if we have 2 prices that are the same it will count as 1
         :where [?product :product/price ?price]]
       db))

(defn summary-of-products-by-category
  [db]
  (d/q '[:find ?category-name (min ?price) (max ?price) (count ?price) (sum ?price)
         :keys category min-price max-price quantity sum-price
         :with ?product
         :in $ %
         :where [?product :product/price ?price]
                (product-in-category ?product ?category-name)]
       db rules))

(defn search-all-of-ip
  [db ip]
  (d/q '[:find [(pull ?product [*]) ...]
         :in $ ?searched-ip
         :where [?transaction :tx-data/ip ?searched-ip]
                [?product :product/id _ ?transaction]]
       db ip))