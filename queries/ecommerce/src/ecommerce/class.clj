(ns ecommerce.class
  (:use clojure.pprint)
  (:require [ecommerce.db.product :as db.product]
            [ecommerce.db.config :as db.config]
            [datomic.api :as d]
            [schema.core :as s]
            [ecommerce.model :as model]))

(s/set-fn-validation! true)

(db.config/delete-database!)
(def conn (db.config/open-connection!))
(db.config/create-schema! conn)

(db.config/create-sample-data! conn)

(pprint (db.product/search-all (d/db conn)))

(pprint (db.product/search-all-by-category (d/db conn) "Eletronics"))

(pprint (db.product/search-all-by-category-backward (d/db conn) "Eletronics"))

(pprint @(db.product/upsert! conn [{:product/id       (model/uuid)
                                    :product/slug     "/t-shirt"
                                    :product/price    30M
                                    :product/name     "T-shirt"
                                    :product/category {:category/name "Clothes"
                                                       :category/id   (model/uuid)}}] "200.216.222.125"))

(pprint (db.product/search-all (d/db conn)))

;(pprint @(db.product/upsert-without-model! conn [{:product/id       (model/uuid)
;                                                  :product/slug     "/checkers"
;                                                  :product/price    15M
;                                                  :product/name     "Checkers"
;                                                  :product/category [:category/id (parse-uuid "27914b3b-dd76-427c-8b03-a1f5228aeb34")]}]))
(pprint (db.product/search-all (d/db conn)))

(pprint (db.product/summary-of-products (d/db conn)))
(pprint (db.product/summary-of-products-by-category (d/db conn)))

(pprint (db.product/search-most-expensive (d/db conn)))
(pprint (db.product/search-cheapest (d/db conn)))

(pprint (db.product/search-all-of-ip (d/db conn) "200.216.222.125"))