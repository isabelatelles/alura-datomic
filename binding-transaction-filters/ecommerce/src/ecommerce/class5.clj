(ns ecommerce.class5
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

;code is executed in transactor!
(def increment-visualization
  #db/fn { :lang :clojure
           :params [db product-id]
           :code
           (let [visualizations (d/q '[:find ?visualizations .
                                        :in $ ?id
                                        :where [?product :product/id ?id]
                                               [?product :product/visualizations ?visualizations]]
                                        db product-id)
                 current (or visualizations 0)
                 total-new (inc current)]
             [{:product/id             product-id
               :product/visualizations total-new}])})       ; returns datoms that we want to transact

(pprint @(d/transact conn [{:db/doc   "increment attribute :product/visualizations"
                            :db/ident :increment-visualization
                            :db/fn    increment-visualization}]))

(dotimes [_ 10] (db.product/visualization! conn (:product/id first-product)))
(pprint (db.product/search-one (d/db conn) (:product/id first-product)))