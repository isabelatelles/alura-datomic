(ns ecommerce.class3
  (:use clojure.pprint)
  (:require [ecommerce.db.config :as db.config]
            [ecommerce.db.product :as db.product]
            [schema.core :as s]
            [datomic.api :as d]
            [schema-generators.generators :as g]
            [ecommerce.model :as model]
            [ecommerce.generators :as generators]))

(s/set-fn-validation! true)

(db.config/delete-database!)
(def conn (db.config/open-connection!))
(db.config/create-schema! conn)
(db.config/create-sample-data! conn)

(pprint (count (db.product/search-all (d/db conn))))

(defn generate-10000-products
  [conn]
  (dotimes [_ 50]
    (def generated-products (g/sample 200 model/Product generators/leaf-generators))
    (db.product/upsert! conn generated-products)))

(time (generate-10000-products conn))

(pprint (count (db.product/search-all (d/db conn))))

(println "search most expensive price")
(time (pprint (db.product/search-most-expensive-price (d/db conn))))
(println "search most expensive price than")
(time (pprint (count (db.product/search-most-expensive-price-than (d/db conn) 50000M))))
(println "search by price")
; added index AVET (https://docs.datomic.com/pro/query/indexes.html#avet)
; so time went down to half
; it searches first the attribute, then value
(time (pprint (db.product/search-by-price (d/db conn) 50000M)))
; indexes increase memory consumption
; and also it increases the time to write

; plan of action with
; order of price first reduces time in almost 4x!!
; always from more restrictive to less restrictive!
(time (pprint (db.product/search-by-price-and-name (d/db conn) 1000M "com")))