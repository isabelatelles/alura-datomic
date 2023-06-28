(ns ecommerce.class3
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

(pprint @(db.product/add-variation! conn (:product/id first-product) "Season pass" 40M))
(pprint @(db.product/add-variation! conn (:product/id first-product) "Season pass 4 years" 60M))

(pprint (db.product/search-all (d/db conn)))

;(pprint (d/pull (d/db conn) '[* {:product/category [*]}] [:product/id (:product/id first-product)]))
(pprint (db.product/search-one (d/db conn) (:product/id first-product)))

(println (db.product/total-products (d/db conn)))
(pprint @(db.product/remove! conn (:product/id first-product)))
(println (db.product/total-products (d/db conn)))

; variation was removed as well!!
;
(pprint (d/q '[:find ?name
               :where [_ :variation/name ?name]]
             (d/db conn)))