(ns pmmt.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [pmmt.core-test]))

(doo-tests 'pmmt.core-test)

