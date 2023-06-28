(ns ecommerce.db
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [schema.core :as s]
            [ecommerce.model :as model]
            [clojure.walk :as walk]))

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

             {:db/ident       :category/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :category/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}])

(defn create-schema!
  [connection]
  (d/transact connection schema))

(s/defn upsert-products!
  [connection
   products :- [model/Product]]
  (d/transact connection products))

(s/defn upsert-categories!
  [connection
   categories :- [model/Category]]
  (d/transact connection categories))

(defn reduce-products-to-db-adds-assigning-product-categories
  [products category]
  (reduce
    (fn [db-adds product]
      (conj db-adds [:db/add                                ; :db/add assigning a category for each product
                     [:product/id (:product/id product)]    ; product id (entity id)
                     :product/category                      ; :product/category (attribute)
                     [:category/id (:category/id category)]])) ; category id (value)
    []                                                      ; initial db-adss, empty
    products))

(defn assign-categories!
  [connection products category]
  (let [transaction (reduce-products-to-db-adds-assigning-product-categories products category)]
    (d/transact connection transaction)))

(defn create-sample-data
  [connection]
  (def eletronics (model/new-category "Eletronics"))
  (def sports (model/new-category "Sports"))
  (pprint @(upsert-categories! connection [eletronics, sports]))

  (def computer (model/new-product (model/uuid), "New Computer", "/new-computer", 2500.10M, 10))
  (def phone (model/new-product (model/uuid), "Expensive Phone", "/phone", 888888.10M))
  ;(def calculator {:product/name "Calculator"})
  (def cheap-phone (model/new-product "Cheap Phone", "/cheap-phone", 0.1M))
  (def chess (model/new-product (model/uuid), "Chess Board", "/chess-board", 30M, 5))
  (def game (assoc (model/new-product (model/uuid), "Mario Kart", "/game-mario", 300M) :product/digital true))
  (pprint @(upsert-products! connection [computer, phone, cheap-phone, chess, game]))

  (assign-categories! connection [computer, phone, cheap-phone, game] eletronics)
  (assign-categories! connection [chess] sports))

; simple version that does not deal with it recursive (if theres a map inside it with db/id as well)
(defn simple-datomic-to-entity
  [datomic-entities]
  (map #(dissoc % :db/id) datomic-entities))

(defn dissoc-db-id
  [datomic-entity]
  (if (map? datomic-entity)
    (dissoc datomic-entity :db/id)
    datomic-entity))

(defn datomic-to-entity
  [datomic-entities]
  (walk/prewalk dissoc-db-id datomic-entities))

(def rules
  '[
    [(stock ?product ?stock)
     [?product :product/stock ?stock]]
    [(stock ?product ?stock)
     [?product :product/digital true]
     [(ground ##Inf) ?stock]]                               ; could have defined 100 instead of ##Inf but truth is stock is infinity for digital products
    [(salable? ?product)
     (stock ?product ?stock)
     [(> ?stock 0)]]
    ])

; default return:
; [[{category1}], [{category2}]]
; because if we only choose to find name, price we would have
; [[name, price], [name, price]]
; what we actually need is:
; [{category1}, {category2}]
(s/defn search-all-categories :- [model/Category]
  [db]
  (datomic-to-entity
    (d/q '[:find [(pull ?entity [*]) ...] ; if we dont put ... it will bring only ONE category
           :where [?entity :category/name]] db)))

(s/defn search-all-products :- [model/Product]
  [db]
  (datomic-to-entity
    (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
           :where [?product :product/name]] db)))

(s/defn search-all-salable-products :- [model/Product]
  [db]
  (datomic-to-entity
    (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
           :in $ %
           :where (salable? ?product)]
         db rules)))

(s/defn search-one-salable-product :- (s/maybe model/Product)
  [db
   product-id :- java.util.UUID]
  (let [query '[:find (pull ?product [* {:product/category [*]}]) . ; dot returns product without the vector, see https://docs.datomic.com/pro/query/query.html#find-specifications
                :in $ % ?id
                :where [?product :product/id ?id]
                       (salable? ?product)]
        result (d/q query db rules product-id)
        product (datomic-to-entity result)]
    (if (:product/id product)
      product
      nil)))

; maybe allows nils, and nils allow null pointer exception
; nullpointerexception allows a hell of exceptions
; so we use maybe ONLY in function returns and
; when it makes a lot of sense
; maybe is NOT used in maps!!!
(s/defn search-one-product :- (s/maybe model/Product)
  [db
   product-id :- java.util.UUID]
  (let [result (d/pull db '[* {:product/category [*]}] [:product/id product-id])
        product (datomic-to-entity result)]
    (if (:product/id product)
      product
      nil)))


(s/defn search-one-product! :- model/Product
  [db
   product-id :- java.util.UUID]
  (let [product (search-one-product db product-id)]
    (if product
      product
      (throw (ex-info "Did not find an entity."
                      {:type :errors/not-found,
                       :id product-id})))))