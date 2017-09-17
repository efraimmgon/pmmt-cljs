(ns pmmt.utils)

(defn domap
  "Implementation of Common Lisp `mapc`. It is like `map` except that the
   results of applying function are not accumulated. The `colls` argument
   is returned."
  [f & colls]
  (reduce (fn [_ args]
            (apply f args))
          nil (apply map list colls))
  colls)

(defn deep-merge-with [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))
