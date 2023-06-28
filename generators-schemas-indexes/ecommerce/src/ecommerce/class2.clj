(ns ecommerce.class2
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

(time (pprint (db.product/search-most-expensive-price (d/db conn))))
(time (pprint (count (db.product/search-most-expensive-price-than (d/db conn) 50000M))))