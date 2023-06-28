(ns ecommerce.class1
  (:use clojure.pprint)
  (:require [ecommerce.db.config :as db.config]
            [ecommerce.db.product :as db.product]
            [schema.core :as s]
            [datomic.api :as d]
            [ecommerce.model :as model]))

(s/set-fn-validation! true)

(db.config/delete-database!)
(def conn (db.config/open-connection!))
(db.config/create-schema! conn)
(db.config/create-sample-data! conn)

(pprint (db.product/search-all-by-categories (d/db conn) ["Eletronics", "Food"]))
(pprint (db.product/search-all-by-categories (d/db conn) ["Eletronics", "Sports"]))
(pprint (db.product/search-all-by-categories (d/db conn) ["Sports"]))
(pprint (db.product/search-all-by-categories (d/db conn) []))
(pprint (db.product/search-all-by-categories (d/db conn) ["Food"]))

(pprint (db.product/search-all-by-categories-and-digital (d/db conn)
                                                         ["Eletronics"]
                                                         true))
(pprint (db.product/search-all-by-categories-and-digital (d/db conn)
                                                         ["Eletronics"]
                                                         false))