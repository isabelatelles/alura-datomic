(ns ecommerce.class2
  (:use clojure.pprint)
  (:require [ecommerce.db.config :as db.config]
            [ecommerce.db.product :as db.product]
            [schema.core :as s]
            [datomic.api :as d]))

(s/set-fn-validation! true)

(db.config/delete-database!)
(def conn (db.config/open-connection!))
(db.config/create-schema! conn)
(db.config/create-sample-data! conn)

(def products (db.product/search-all (d/db conn)))
(def first-product (first products))
(pprint first-product)

(pprint @(db.product/update-price! conn (:product/id first-product) 2500.10M 30M))
(pprint @(db.product/update-price! conn (:product/id first-product) 30M 35M))
; compare failed because old price was 35M
;(pprint @(db.product/update-price! conn (:product/id first-product) 30M 45M))

(def second-product (second products))
(pprint second-product)
(pprint @(db.product/update! conn second-product {:product/id    (:product/id second-product)
                                                  :product/price 10M
                                                  :product/stock 10}))
(pprint (db.product/search-one! (d/db conn) (:product/id second-product)))

;wont work!!!
(pprint @(db.product/update! conn second-product {:product/id    (:product/id second-product)
                                                  :product/price 10M
                                                  :product/stock 10}))