(ns ecommerce.db.sell
  (:require [datomic.api :as d]
            [ecommerce.model :as model]))

(defn insert!
  [conn product-id quantity]
  (let [id (model/uuid)]
    (d/transact conn [{:db/id          "sell"
                       :sell/product   [:product/id product-id]
                       :sell/quantity  quantity
                       :sell/id        id
                       :sell/situation "new"}])
    id))