(ns ecommerce.db.config
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.model :as model]
            [ecommerce.db.product :as db.product]
            [ecommerce.db.category :as db.category]))

(def db-uri "datomic:dev://localhost:4334/ecommerce")

(defn open-connection!
  []
  (d/create-database db-uri)
  (d/connect db-uri))

(defn delete-database!
  []
  (d/delete-database db-uri))

(def schema [{:db/ident       :product/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "Name of a product"}
             {:db/ident       :product/slug
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "Path to access a product via http"}
             {:db/ident       :product/price
              :db/valueType   :db.type/bigdec
              :db/cardinality :db.cardinality/one
              :db/index       true
              :db/doc         "Price of a product with monetary precision"}
             {:db/ident       :product/keyword
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/many}
             {:db/ident       :product/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             {:db/ident       :product/category
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}
             {:db/ident       :product/stock
              :db/valueType   :db.type/long
              :db/cardinality :db.cardinality/one}
             {:db/ident       :product/digital
              :db/valueType   :db.type/boolean
              :db/cardinality :db.cardinality/one}
             {:db/ident       :product/variation
              :db/valueType   :db.type/ref
              :db/isComponent true                          ; variation does not exist without product, so pull of product * already brings variation
              :db/cardinality :db.cardinality/many}
             {:db/ident       :product/visualizations
              :db/valueType   :db.type/long
              :db/cardinality :db.cardinality/one
              :db/noHistory   true}

             {:db/ident       :variation/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             {:db/ident       :variation/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :variation/price
              :db/valueType   :db.type/bigdec
              :db/cardinality :db.cardinality/one}

             {:db/ident       :category/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :category/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}

             {:db/ident       :sell/product
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}
             {:db/ident       :sell/quantity
              :db/valueType   :db.type/long
              :db/cardinality :db.cardinality/one}
             {:db/ident       :sell/situation
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :sell/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}])

(defn create-schema!
  [connection]
  (d/transact connection schema))

(defn create-sample-data!
  [connection]
  (def eletronics (model/new-category "Eletronics"))
  (def sports (model/new-category "Sports"))
  (db.category/upsert! connection [eletronics, sports])

  (def computer (model/new-product (model/uuid), "New Computer", "/new-computer", 2500.10M, 10))
  (def phone (model/new-product (model/uuid), "Expensive Phone", "/phone", 888888.10M))
  ;(def calculator {:product/name "Calculator"})
  (def cheap-phone (model/new-product "Cheap Phone", "/cheap-phone", 0.1M))
  (def chess (model/new-product (model/uuid), "Chess Board", "/chess-board", 30M, 5))
  (def game (assoc (model/new-product (model/uuid), "Mario Kart", "/game-mario", 300M) :product/digital true))
  (db.product/upsert! connection [computer, phone, cheap-phone, chess, game])

  (db.category/assign! connection [computer, phone, cheap-phone, game] eletronics)
  (db.category/assign! connection [chess] sports))