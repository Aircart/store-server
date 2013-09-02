(ns store-server.util)

(defn plu? [code]
  (< 5 (.length (name code))))

(defn random-string [length]
  (let [ascii-codes (concat (range 48 58)    ; 0-9
                            (range 65 91)    ; A-Z
                            (range 97 123))] ; a-z
    (apply str (repeatedly length #(char (rand-nth ascii-codes))))))
