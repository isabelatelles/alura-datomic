(ns ecommerce.db.entity
  (:require [clojure.walk :as walk]))

(defn dissoc-db-id
  [datomic-entity]
  (if (map? datomic-entity)
    (dissoc datomic-entity :db/id)
    datomic-entity))

(defn datomic-to-entity
  [datomic-entities]
  (walk/prewalk dissoc-db-id datomic-entities))