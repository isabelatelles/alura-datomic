(ns ecommerce.class5
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.db :as db]
            [ecommerce.model :as model]))

(db/deletes-database)

(def conn (db/open-connection))

(db/create-schema conn)

(let [computer (model/new-product "New Computer", "/new-computer", 2500.10M)
      phone (model/new-product "Expesive Phone", "/phone", 888888.10M)
      result @(d/transact conn [computer, phone])]
  (pprint result))

(def snapshot-of-past (d/db conn))

(let [calculator {:product/name "Calculator"}
      cheap-phone (model/new-product "Cheap Phone", "/cheap-phone", 0.1M)]
  (d/transact conn [calculator, cheap-phone]))

; should be 4
(pprint (count (db/search-all-products (d/db conn))))
; should be 2
(pprint (count (db/search-all-products snapshot-of-past)))

; should be 0
(pprint (count (db/search-all-products (d/as-of (d/db conn) #inst "2023-06-21T20:11:29.218-00:00"))))
; should be 2
(pprint (count (db/search-all-products (d/as-of (d/db conn) #inst "2023-06-21T20:11:55.218-00:00"))))
; should be 4
(pprint (count (db/search-all-products (d/as-of (d/db conn) #inst "2023-06-21T20:12:13.500-00:00"))))