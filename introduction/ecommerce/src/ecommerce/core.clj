(ns ecommerce.core
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.db :as db]
            [ecommerce.model :as model]))

(db/deletes-database)
(def conn (db/open-connection))
(db/create-schema conn)

(let [computer (model/new-product "New Computer", "/new_computer", 2500.10M)]
  (d/transact conn [computer]))

; database in the instant the line is executed
(def database-with-computer (d/db conn))

; get entities that have product/name as attribute
(d/q '[:find ?entity
       :where [?entity :product/name]] database-with-computer)

(let [phone (model/new-product "Expesive Phone", "/phone", 888888.10M)]
  (d/transact conn [phone]))

(d/q '[:find ?entity
       :where [?entity :product/name]] database-with-computer)

; new SNAPSHOT of the database
(def database-with-computer-and-phone (d/db conn))

(d/q '[:find ?entity
       :where [?entity :product/name]] database-with-computer-and-phone)

; we can access the database in any moment in time (past, future, etc)

(let [calculator {:product/name "Calculator"}]
  (d/transact conn [calculator]))

(let [cheap-phone (model/new-product "Cheap Phone", "/cheap-phone", 8888.1M)
      result @(d/transact conn [cheap-phone]) ; transaction is made with future, its async so we need to deref in order to get the actual result when transaction finishes
      entity-id (-> result :tempids vals first)]
  (pprint result)
  (pprint @(d/transact conn [[:db/add entity-id :product/price 0.1M]]))
  (pprint @(d/transact conn [[:db/retract entity-id :product/slug "/cheap-phone"]]))) ; makes a retract of old price and adds new price