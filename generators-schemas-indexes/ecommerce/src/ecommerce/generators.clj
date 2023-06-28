(ns ecommerce.generators
  (:require [clojure.test.check.generators :as gen]))

(defn double-to-big-decimal
  [value]
  (BigDecimal. value))

(def double-finite (gen/double* {:infinite? false, :NaN? false}))
(def big-decimal (gen/fmap double-to-big-decimal
                           double-finite))

(def leaf-generators { BigDecimal big-decimal })