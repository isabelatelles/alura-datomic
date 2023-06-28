(ns ecommerce.db.category
  (:require [datomic.api :as d]
            [schema.core :as s]
            [ecommerce.model :as model]
            [ecommerce.db.entity :as db.entity]))

(s/defn upsert!
  [connection
   categories :- [model/Category]]
  (d/transact connection categories))

(defn- reduce-products-to-db-adds-assigning-product-categories
  [products category]
  (reduce
    (fn [db-adds product]
      (conj db-adds [:db/add                                ; :db/add assigning a category for each product
                     [:product/id (:product/id product)]    ; product id (entity id)
                     :product/category                      ; :product/category (attribute)
                     [:category/id (:category/id category)]])) ; category id (value)
    []                                                      ; initial db-adss, empty
    products))

(defn assign!
  [connection products category]
  (let [transaction (reduce-products-to-db-adds-assigning-product-categories products category)]
    (d/transact connection transaction)))

(s/defn search-all :- [model/Category]
  [db]
  (db.entity/datomic-to-entity
    (d/q '[:find [(pull ?entity [*]) ...] ; if we dont put ... it will bring only ONE category
           :where [?entity :category/name]] db)))