(ns ecommerce.class6
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
  (pprint @(d/transact conn [computer, phone, calculator, cheap-phone])))

(pprint (db/search-all-products-by-price (d/db conn) 1000))
(pprint (db/search-all-products-by-price (d/db conn) 5000))

(d/transact conn [[:db/add 17592186045418 :product/keyword "desktop"]
                  [:db/add 17592186045418 :product/keyword "computer"]])
(pprint (db/search-all-products (d/db conn)))

(d/transact conn [[:db/retract 17592186045418 :product/keyword "computer"]])
(pprint (db/search-all-products (d/db conn)))

(d/transact conn [[:db/add 17592186045419 :product/keyword "phone"]
                  [:db/add 17592186045421 :product/keyword "phone"]])
(pprint (db/search-all-products (d/db conn)))

(pprint (db/search-all-products-by-keyword (d/db conn) "phone"))
(pprint (db/search-all-products-by-keyword (d/db conn) "computer"))