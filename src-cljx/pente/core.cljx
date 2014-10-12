(ns pente.core
  (:require
   #+clj  [schema.core :as s]
   #+cljs [schema.core :as s :include-macros true]))

;; Define Constants

(def ^:const BOARD_SIZE "Number of playable points on the side of of the board"
  19)

(def ^:const PLAYER_NAMES "Names of the players of the game."
  ["Black" "White"])

;; Define Schemas

(defn Vector
  "A schema like a regular sequence which must also be a vector"
  [& schemas]
  (apply s/both
         (s/pred vector? "vector?")
         schemas))

(defn Grid
  "A schema for a two dimensional vector of schemas."
  [& schemas]
  (Vector [(Vector (vec schemas))]))

(def Location "A Schema representing a location on a pente board."
  (Vector [(s/one s/Int "x") (s/one s/Int "y")]))

;; Define Data Types

(s/defrecord Player [name :- s/Str
                     captures :- s/Int])

(s/defrecord Piece [player :- s/Int
                    location :- Location])

(s/defrecord Game [players :- (Vector [Player])
                   pieces :- [Piece]
                   current-player :- s/Int
                   winner :- (s/maybe s/Int)])

;; Begin defining tools for creating instances of the data types.

(s/defn make-player :- Player
  "Create a new player with the given name."
  [name :- s/Str]
  (->Player name 0))

(s/defn add-player :- Game
  "Adds a new player with the given name to the game."
  [game :- Game
   name :- s/Str]
  (update-in game [:players] conj (make-player name)))

(s/defn add-players :- Game
  "Creates players with the given name and adds them to the game."
  [game :- Game
   player-names :- [s/Str]]
  (reduce add-player game player-names))

(s/defn make-game :- Game
  "Creates a blank new game"
  []
  (-> (->Game [] [] 0 nil)
      (add-players PLAYER_NAMES)))

;; Begin the locig related to making moves.

(s/defn increment-current-player :- Game
  "This function just sets the current player to be the next player in the order"
  [{:keys [players current-player] :as game} :- Game]
  (let [number-players (count players)
        next-player (mod (inc current-player) number-players)]
    (assoc-in game [:current-player] next-player)))

(s/defn truthy? :- s/Bool
  "Converts a value to its clojure truth value."
  [arg :- s/Any]
  (if arg
    true
    false))

(s/defn occupied? :- s/Bool
  "This indicates whether a piece is already at the specified location."
  [{:keys [pieces] :as game} :- Game
   [x y :as location] :- Location]
  (->> pieces
       (map :location)
       (some #{location})
       truthy?))

;; Now begins the task of dealing with the capturing logic.

(def CARDINAL_DIRECTIONS_BASE "This will be used in calculating directions."
  [-1 0 1])

(def CAPTURE_OFFSETS "This holds all the sets of offsets that are necessary for determining
  locations relevant for capturing. It is a seq where each element of the seq is a seq containing the locations
  for that particular directions capturing locations. It includes the offsets for the enemy locations sandwiched
  between the friendly locations."
  (for [x CARDINAL_DIRECTIONS_BASE
        y CARDINAL_DIRECTIONS_BASE
        :when (not (== 0 x y))]
    (map (fn [multiplier]
           [(* x multiplier) (* y multiplier)])
         (range 4))))

(s/defn get-capture-locations :- [[Location]]
  "Returns the CAPTURE_OFFSETS offset for the particular location in question."
  [game :- Game
   location :- Location]
  (map (fn [offset-group]
         (map #(mapv + % location) offset-group))
       CAPTURE_OFFSETS))

(s/defn get-piece-at-location :- (s/maybe Piece)
  "Returns the piece at the specified location. nil if there is no piece."
  [{:keys [pieces] :as game} :- Game
   location :- Location]
  (->> pieces
       (filter (fn [{:keys [player]
                     piece-location :location}]
                 (= location piece-location)))
       first))

(s/defn friendly? :- s/Bool
  "Is there a piece friendly to player at location?"
  [{:keys [pieces] :as game} :- Game
   player :- s/Int
   location :- Location]
  (let [piece (get-piece-at-location game location)]
    (= player (:player piece))))

(s/defn enemy? :- s/Bool
  "Is there an enemy piece of player at location?"
  [{:keys [pieces] :as game} :- Game
   player :- s/Int
   location :- Location]
  (let [piece (get-piece-at-location game location)]
    (truthy? (and piece
                  (not= player (:player piece))))))

(s/defn do-captures-for-locations :- Game
  "This will attempt to capture any pieces for Player.
  Locations is the list of locations we need to look at.
  The first and last locations should have friendly pieces and the rest should be enemy in order for a capture to succeed."
  [game :- Game
   player :- s/Int
   locations :- [Location]]
  (let [friendlies [(first locations) (last locations)]
        enemies (->> locations rest butlast)]
    (if (and (every? #(friendly? game player %) friendlies)
             (every? #(enemy? game player %) enemies))
      (-> game
          (update-in [:players player :captures] inc)
          (update-in [:pieces] (fn [pieces]
                                 (remove (fn [{:keys [location]}]
                                           (some #{location} enemies))
                                         pieces))))
      game)))

(s/defn do-captures :- Game
  "This will attempt to perform any captures that need to happen for a player who just played at location."
  [game :- Game
   player :- s/Int
   [x y :as location] :- Location] ;; This COULD assume that the player is the current player, but it might not have been incremented yet
  (let [capture-locations (get-capture-locations game location)]
    (reduce #(do-captures-for-locations %1 player %2) game capture-locations)))

;; This section will cover the rules of winning.

(s/defn five-captures-winner :- (s/maybe s/Int)
  "This function returns a player who has 5 captures."
  [{:keys [players]} :- Game]
  (->> players
       (map :captures)
       (map #(<= 5 %))
       (map-indexed (fn [index value]
                      (if value index)))
       (drop-while nil?)
       first))

;; This section will handle the overly complex task of determining whether somebody has 5 in a row.

(s/defn make-blank-grid :- (Grid (s/enum nil))
  "This function will make a size by size grid with all values initialized to nil."
  [size :- s/Int]
  (->> (repeat size nil)
       vec
       (iterate vec)
       (take size)
       vec))

(s/defn convert-pieces-to-grid :- (Grid (s/maybe s/Int))
  "This function converts the pieces to a 2-dimensional vector where the values are the player who has a stone at that location."
  [pieces :- [Piece]]
  (let [grid (make-blank-grid BOARD_SIZE)]
    (reduce (fn [grid {:keys [player location]}]
              (assoc-in grid (rseq location) player))
            grid
            pieces)))

(s/defn transpose :- (Grid s/Any)
  "This function transposes a 2-dimensional grid."
  [grid :- (Grid s/Any)]
  (let [old-width (count (first grid))]
    (reduce (fn [arr old-col]
              (conj arr (mapv #(get % old-col) grid)))
            []
            (range old-width))))

(s/defn diagonal :- [s/Any]
  "This function returns the diagonal of a matrix starting at start-location and going down and towards the right."
  [grid :- (Grid s/Any)
   [x y :as start-location] :- Location]
  (let [y-max (count grid)
        x-max (count (first grid))]
    (loop [x x
           y y
           acc []]
      (if (or (<= x-max x)
              (<= y-max y))
        acc
        (recur (inc x)
               (inc y)
               (conj acc (get-in grid [y x])))))))

(s/defn south-west-diagonals :- [s/Any]
  "This function will return all the northeast to southwest diagonals in grid."
  [grid :- (Grid s/Any)]
  (let [x-max (count grid)
        y-max (count (first grid))
        top-starts (map #(vector % 0) (range x-max))
        left-starts (map #(vector 0 %) (range 1 y-max))
        all-starts (concat top-starts left-starts)]
    (map #(diagonal grid %) all-starts)))

(s/defn south-east-diagonals
  "This function returns all the northwest to southeast diagonals in grid."
  [grid :- (Grid s/Any)]
  (let [x-max (count (first grid))
        y-max (count grid)
        top-starts (map #(vector % 0) (range x-max))
        right-starts (map #(vector x-max %) (range 1 y-max))]))

(s/defn get-all-lines :- [[s/Any]]
  "This will get all lines for a grid including rows, columns and both sets of diagonals."
  [grid :- (Grid s/Any)]
  (let [trans (transpose grid)
        sw-diags (south-west-diagonals grid)
        se-diags (south-west-diagonals (mapv (comp vec rseq) trans))]
    (concat grid trans sw-diags se-diags)))

(s/defn n-consecutive :- (s/maybe s/Int)
  "This function will return a value if the value appears n consecutive times in the collection"
  [n :- s/Int
   coll :- [(s/maybe s/Int)]]
  (loop [[x & xs :as coll] coll
         cur-item nil
         cur-count 0]
    (cond (>= cur-count n) cur-item
          (empty? coll) nil
          (nil? x) (recur xs x cur-count)
          (= x cur-item) (recur xs cur-item (inc cur-count))
          :else (recur xs x 1))))

(s/defn five-in-a-row-winner :- (s/maybe s/Int)
  "This function returns a player who has 5 in a row"
  [{:keys [pieces]} :- Game]
  (let [grid (convert-pieces-to-grid pieces)
        lines (get-all-lines grid)]
    (some #(n-consecutive 5 %) lines)))

(s/defn get-winner :- (s/maybe s/Int)
  "This function returns the winner of the game or nil if there is not one."
  [game]
  (or (five-captures-winner game)
      (five-in-a-row-winner game)))

;; Final winner-checking function

(s/defn check-winner :- Game
  "Checks to see if there is a winner and if so sets the winner in the game."
  [game :- Game]
  (if-let [winner (get-winner game)]
    (assoc-in game [:winner] winner)
    game))

;; Validation Functions

(s/defn within-bounds? :- s/Bool
  "Verify that the location is a valid board location."
  [[x y] :- Location]
  (and (<= 0 x (dec BOARD_SIZE))
       (<= 0 y (dec BOARD_SIZE))))

;; Finally what the rest of the file has been building up to, the move function.

(s/defn move :- Game
  "This is the most important function for users outside the namespace to call.
  It tries to place a piece at location."
  [{:keys [current-player winner] :as game} :- Game
   [x y :as location] :- Location]
  (cond
   winner game
   ((complement within-bounds?) location) game
   (occupied? game location) game
   :else (-> game
             (update-in [:pieces] conj (->Piece current-player [x y]))
             (do-captures current-player location)
             check-winner
             increment-current-player)))
