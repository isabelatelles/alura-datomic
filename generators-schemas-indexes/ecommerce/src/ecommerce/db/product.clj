(ns ecommerce.db.product
  (:require [datomic.api :as d]
            [schema.core :as s]
            [ecommerce.model :as model]
            [ecommerce.db.entity :as db.entity]))

(s/defn upsert!
  [connection
   products :- [model/Product]]
  (d/transact connection products))

(s/defn search-all :- [model/Product]
  [db]
  (db.entity/datomic-to-entity
    (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
           :where [?product :product/id]] db)))

(s/defn search-one :- (s/maybe model/Product)
  [db
   product-id :- java.util.UUID]
  (let [result (d/pull db '[* {:product/category [*]}] [:product/id product-id])
        product (db.entity/datomic-to-entity result)]
    (if (:product/id product)
      product
      nil)))


(s/defn search-one! :- model/Product
  [db
   product-id :- java.util.UUID]
  (let [product (search-one db product-id)]
    (if product
      product
      (throw (ex-info "Did not find an entity."
                      {:type :errors/not-found,
                       :id product-id})))))

(s/defn search-most-expensive-price :- BigDecimal
  [db]
  (d/q '[:find (max ?price) .
         :where [_ :product/price ?price]]
       db))

(defn search-most-expensive-price-than
  [db min-price]
  (d/q '[:find ?price
         :in $ ?min
         :where [_ :product/price ?price]
                [(>= ?price ?min)]]
       db min-price))

(defn search-by-price
  [db price]
  (db.entity/datomic-to-entity
    (d/q '[:find (pull ?product [*])
           :in $ ?search-price
           :where [?product :product/price ?search-price]]
         db price)))

(defn search-by-price-and-name
  [db price name]
  (db.entity/datomic-to-entity
    (d/q '[:find (pull ?product [*])
           :in $ ?search-price ?substring
           :where [?product :product/price ?search-price]   ; important to be the first one in this order
                  [?product :product/name ?search-name]
                  [(.contains ?search-name ?substring)]]
         db price name)))