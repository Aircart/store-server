(ns store-server.controllers.cards
  (:require [store-server.models.card :as card]))

(defn list-cards [user-id dbd]
  (let [resp-map (card/fetch-all dbd user-id)]
    (if (nil? resp-map)
      { :status 204 }
      { :status 200
        :body   resp-map})))

(defn add-card [params user-id dbd]
  (let [card-id (card/create dbd user-id params)]
    (if (nil? card-id)
      { :status 402 }
      { :status 201
        :headers { "Location" (str "/cards/" card-id) } })))

(defn set-default-card [card-id user-id dbd]
  (if (card/set-default dbd user-id card-id)
    { :status 200 }
    { :status 404 }))

(defn delete-card [card-id user-id dbd]
  (if (card/delete dbd user-id card-id)
    { :status 200 }
    { :status 404 }))
