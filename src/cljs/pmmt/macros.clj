(ns pmmt.macros)

(defmacro log
  [& msgs]
  `(.log js/console ~@msgs))
