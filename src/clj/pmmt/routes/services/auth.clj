(ns pmmt.routes.services.auth
  (:require [pmmt.db.core :as db]
            [pmmt.validation :refer [registration-errors]]
            [ring.util.http-response :as response]
            [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]))

; helpers -----------------------------------------------------

(defn handle-registration-error [e]
  (if (and
       (instance? java.sql.SQLException e)
       (-> e
           (.getNextException)
           (.getMessage)
           (.startsWith "ERROR: duplicate key value")))
    ; username already exists
    (response/precondition-failed
     {:result :error
      :message "user with the selected ID already exists"})
    ; server error
    (do
     (log/error e)
     (response/internal-server-error
      {:result :error
       :message "server error occurred while adding the user"}))))

(defn decode-auth
  "Decode credentials of basic HTTP authentition. [In it the username and the
  password are joined using the `:` separator, and encoded using base 64,
  and the method `Basic()` is prepended; so here we do the reverse path]."
  [encoded]
  (let [auth (second (.split encoded " "))]
    (-> (.decode (java.util.Base64/getDecoder) auth)
        (String. (java.nio.charset.Charset/forName "UTF-8"))
        (.split ":"))))

(defn authenticate
  "Checks if there's a user with the `id` in db, and if the `pass`
  matches db's"
  [[id pass]]
  (when-let [user (db/get-user {:id id})]
    (when (hashers/check pass (:pass user))
      id)))

; core -----------------------------------------------------

(defn register! [request user-data]
  (let [{:keys [session]} request]
    (if (registration-errors user-data)
      (response/precondition-failed {:result :error})
      (try
        (db/create-user!
         (-> user-data
             (dissoc :pass-confirm)
             (update :pass hashers/encrypt)))
        (-> {:result :ok}
            (response/ok)
            (assoc :session (assoc session :identity (:id user-data))))
        (catch Exception e
          (handle-registration-error e))))))

(defn login! [request auth]
  (let [{:keys [session]} request]
    (if-let [id (authenticate (decode-auth auth))]
      (-> {:result :ok}
          (response/ok)
          (assoc :session (assoc session :identity id)))
      (response/unauthorized {:result :unauthorized
                              :message "nome de usuário e/ou senha incorretos"}))))

(defn logout! []
  (-> {:result :ok}
      (response/ok)
      (assoc :session nil)))

(defn delete-account! [identity]
  (db/delete-account! identity)
  (-> {:result :ok}
      (response/ok)
      (assoc :session nil)))
