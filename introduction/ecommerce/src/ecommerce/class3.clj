(ns ecommerce.class3
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.db :as db]
            [ecommerce.model :as model]))

(db/deletes-database)

(def conn (db/open-connection))

(db/create-schema conn)

(let [computer (model/new-product "New Computer", "/new-computer", 2500.10M)
      phone (model/new-product "Expesive Phone", "/phone", 888888.10M)
      calculator {:product/name "Calculator"}
      cheap-phone (model/new-product "Cheap Phone", "/cheap-phone", 0.1M)]
  (d/transact conn [computer, phone, calculator, cheap-phone]))

(pprint (db/search-all-products-entity-id (d/db conn)))
(pprint (db/search-all-products-by-slug (d/db conn) "/new-computer"))
(pprint (db/search-all-slugs (d/db conn)))
(pprint (db/search-all-products-with-name-and-price (d/db conn)))
(pprint (db/search-all-products (d/db conn)))