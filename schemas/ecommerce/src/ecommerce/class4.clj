(ns ecommerce.class4
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

(println (:product/stock (first products)))
(pprint (db/search-one-salable-product
          (d/db conn)
          (:product/id (first products))))

(println (:product/stock (second products)))
(pprint (db/search-one-salable-product
          (d/db conn)
          (:product/id (second products))))

(pprint (db/search-all-salable-products (d/db conn)))