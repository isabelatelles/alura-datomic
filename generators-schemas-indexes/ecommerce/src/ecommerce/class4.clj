(ns ecommerce.class4
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

(defn value-properties
  [value]
  (if (vector? value)
    (merge {:db/cardinality :db.cardinality/many}
           (value-properties (first value)))
    (cond (= value java.util.UUID) {:db/valueType :db.type/uuid
                                    :db/unique    :db.unique/identity}
          (= value s/Str) {:db/valueType :db.type/string}
          (= value BigDecimal) {:db/valueType :db.type/bigdec}
          (= value Long) {:db/valueType :db.type/long}
          (= value s/Bool) {:db/valueType :db.type/boolean}
          (map? value) {:db/valueType :db.type/ref}
          :else {:db/valueType (str "unknown: " (type value) value)})))

(defn extract-key-name
  [key]
  (cond (keyword? key) key
        (instance? schema.core.OptionalKey key) (get key :k)
        :else key))

(defn key-value-to-datomic-schema
  [[key value]]
  (let [base {:db/ident       (extract-key-name key)
              :db/cardinality :db.cardinality/one}
        extra (value-properties value)
        datomic-schema (merge base extra)]
    datomic-schema))

(defn schema-to-datomic
  [definition]
  (map key-value-to-datomic-schema definition))

(pprint (schema-to-datomic model/Category))
(pprint (schema-to-datomic model/Variation))
(pprint (schema-to-datomic model/Sell))
(pprint (schema-to-datomic model/Product))