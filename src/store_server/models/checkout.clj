(ns store-server.models.checkout
  (:require [leveldb-clj.core :as db]))

;;
;; expiration for open checkout
;;

;; user phone
;;


;; checkout

  ;; card pre-authorized?

    ;; send event to checkpoint


;; cancel checkout

  ;; remove card lock

  ;; send event to checkpoint


;; self-finalization -- UNIMPLEMENTED

  ;; verify QR code

    ;; charge card

      ;; generate receipt

    ;; problem charging card



;; checkpoint
;;
;; REST Controller methods:
;; - finalize -> 201 / 302 to verification
;; - confirm  -> 201 / 200 (aborted) / 302 to next verification
;; - abort    -> 200
;; 
;; POST /checkout/finalize
;; PUT  /checkout/verification/[cart|id] -- same as finalization but preprocess verification
;;      { action: [ok|issue|abort] }
;; POST /checkout/abort -- can be both used by user or checkpoint, identified by auth header


;; get open checkouts -- starts websocket


;; verification update


;; checkpoint verification

  ;; no verification needed

  ;; verification needed [cart check, 18/21 ID check]


;; finalize

  ;; charge card
  
    ;; generate receipt

    ;; send push to phone

  ;; problem charging card


;; abort checkout

  ;; remove card lock

  ;; send push to phone
