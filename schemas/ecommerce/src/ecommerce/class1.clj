(ns ecommerce.class1
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.db :as db]
            [schema.core :as s]))

(s/set-fn-validation! true)

(db/delete-database!)
(def conn (db/open-connection!))
(db/create-schema! conn)

(db/create-sample-data conn)

(pprint (db/search-all-categories (d/db conn)))
(pprint (db/search-all-products (d/db conn)))