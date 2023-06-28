(ns ecommerce.db
  (:use clojure.pprint)
  (:require [datomic.api :as d]))

(def db-uri "datomic:dev://localhost:4334/ecommerce")

(defn open-connection
  []
  (d/create-database db-uri)
  (d/connect db-uri))

(defn deletes-database
  []
  (d/delete-database db-uri))

(def schema [{:db/ident       :product/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "Name of a product"}
             {:db/ident       :product/slug
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "Path to access a product via http"}
             {:db/ident       :product/price
              :db/valueType   :db.type/bigdec
              :db/cardinality :db.cardinality/one
              :db/doc         "Price of a product with monetary precision"}
             {:db/ident       :product/keyword
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/many}])

(defn create-schema
  [connection]
  (d/transact connection schema))

(defn search-all-products-entity-id
  [db]
  (d/q '[:find ?entity
         :where [?entity :product/name]] db))

(defn search-all-products-with-name-and-price
  [db]
  (d/q '[:find ?name, ?price
         :keys product/name, product/price
         :where [?entity :product/price ?price]
         [?entity :product/name ?name]]
       db))

(defn search-all-products
  [db]
  (d/q '[:find (pull ?entity [*]) ; (pull ?entity [:product/name, :product/price, :product/slug]) brings attributes of your choice
         :where [?entity :product/name]] db))

(defn search-all-products-by-slug
  [db slug]
  (d/q '[:find ?entity
         :in $ ?slug-to-be-found ; $ represents db
         :where [?entity :product/slug ?slug-to-be-found]]
       db slug))

(defn search-all-slugs
  [db]
  (d/q '[:find ?slug
         :where [_ :product/slug ?slug]]
       db))

; datomic defines indexes, we define indexes, but
; we define plan of actions!!
(defn search-all-products-by-price
  [db minimum-price]
  (d/q '[:find ?name, ?price
         :in $ ?min-price
         ;it is done in THIS order
         :where [?entity :product/price ?price]
                [(> ?price ?min-price)] ; put condition more restrictive first
                [?entity :product/name ?name]]
       db minimum-price))

; 10000 products total
; 1000 products with price > 5000
; only 10 products with quantity < 10
; [(> price 5000)]   => 1000 datoms
; [(< quantity 10)]  => 10 datoms

; [(< quantity 10)]  => 10 datoms
; [(> price 5000)]   => 10 datoms

(defn search-all-products-by-keyword
  [db keyword]
  (d/q '[:find (pull ?entity [*])
         :in $ ?keyword
         :where [?entity :product/keyword ?keyword]]
       db keyword))