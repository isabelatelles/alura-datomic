(ns ecommerce.class3
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

(def products (db/search-all-products (d/db conn)))
(def first-product (first products))
(pprint first-product)

; if it does not find any product, then returns nil
(db/search-one-product (d/db conn) (:product/id first-product))
(db/search-one-product (d/db conn) (model/uuid))            ; invalid id

; if it does not find any product, then throws error, no matter if schema is active or not
(db/search-one-product! (d/db conn) (:product/id first-product))
(db/search-one-product! (d/db conn) (model/uuid))            ;invalid id