(ns pmmt.validation
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]))

; error msgs -------------------------------------------------------------

(defn required-msg [field]
  (str field ": Este campo é obrigatório"))

(defn match-msg [field]
  (str field ": Insira os dados de acordo com o formato exigido"))

(defn number-msg [field]
  (str field ": O argumento deve ser um número"))

; validation components ----------------------------------------------------

(defn required [msg]
  [v/required :message (required-msg msg)])

(defn match-date [msg]
  [v/matches #"\d{2}/\d{2}/\d{4}" :message (match-msg msg)])

; validation functions -----------------------------------------------------

(defn registration-errors [{:keys [pass-confirm] :as params}]
  (first
   (b/validate
    params
    :id v/required
    :pass [v/required
           [v/min-count 7 :message "As senhas devem conter no mínimo 8 caracteres"]
           [= pass-confirm :message "As senhas inseridas não correspondem"]])))

(defn validate-report-args [params]
  (first
   (b/validate
    params
    :data-inicial-a [(required "Data inicial - A")
                     [v/matches #"\d{2}/\d{2}/\d{4}" :message (match-msg "Data inicial - A")]]
    :data-final-a [(required "Data final - A")
                   [v/matches #"\d{2}/\d{2}/\d{4}" :message (match-msg "Data final - A")]]
    :data-inicial-b [(required "Data inicial - B")
                     [v/matches #"\d{2}/\d{2}/\d{4}" :message (match-msg "Data inicial - B")]]
    :data-final-b [(required "Data final - B")
                   [v/matches #"\d{2}/\d{2}/\d{4}" :message (match-msg "Data final - B")]])))

(defn validate-map-args [params]
  (first
   (b/validate
    params
    :data_inicial [[v/required :message (required-msg "Data inicial")]
                   [v/matches #"\d{2}/\d{2}/\d{4}" :message (match-msg "Data inicial")]]
    :data_final [[v/required :message (required-msg "Data final")]
                 [v/matches #"\d{2}/\d{2}/\d{4}" :message (match-msg "Data final")]]
    :hora_inicial [[v/matches #"\d{2}:\d{2}" :message (match-msg "Hora inicial")]]
    :hora_final [[v/matches #"\d{2}:\d{2}" :message (match-msg "Hora final")]])))

(defn validate-util-date-calc [params]
  (first
   (b/validate
    params
    :date [[v/required :message (required-msg "Data de início")]
           [v/matches #"\d{2}/\d{2}/\d{4}" :message (match-msg "Data de início")]]
    :days [[v/required :message (required-msg "Quantidade de dias")]
           [v/matches #"\d+" :message (number-msg "Quantidade de dias")]])))
