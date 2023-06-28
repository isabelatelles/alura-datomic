(ns ecommerce.model
  (:require [schema.core :as s]))

(defn uuid [] (java.util.UUID/randomUUID))

(def Category
  {:category/id   java.util.UUID
   :category/name s/Str})

(def Variation
  {:variation/id    java.util.UUID
   :variation/name  s/Str
   :variation/price BigDecimal})

(def Product
  {:product/id                              java.util.UUID
   (s/optional-key :product/name)           s/Str
   (s/optional-key :product/slug)           s/Str
   (s/optional-key :product/price)          BigDecimal
   (s/optional-key :product/keyword)        [s/Str]
   (s/optional-key :product/category)       Category
   (s/optional-key :product/stock)          Long
   (s/optional-key :product/digital)        s/Bool
   (s/optional-key :product/variation)      [Variation]
   (s/optional-key :product/visualizations) Long})

(def Sell
  {:sell/id                         java.util.UUID
   (s/optional-key :sell/product)   Product
   (s/optional-key :sell/quantity)  Long
   (s/optional-key :sell/situation) s/Str})

(defn new-category
  ([name]
   (new-category (uuid) name))
  ([uuid name]
   {:category/id   uuid
    :category/name name}))

(defn new-product
  ([name slug price]
   (new-product (uuid) name slug price))
  ([uuid name slug price]
   (new-product uuid name slug price 0))
  ([uuid name slug price stock]
   {:product/id      uuid
    :product/name    name
    :product/slug    slug
    :product/price   price
    :product/stock   stock
    :product/digital false}))