(ns pente.core)

;; Define Constants

(def ^:const BOARD_SIZE "Number of playable points on the side of of the board"
  19)

(def ^:const PLAYER_NAMES "Names of the players of the game."
  ["Black" "White"])

;; Define Data Types

;; players is a vector holding Player.
;; pieces is a vector holding pieces.
;; current-player is the index into the players vector.
(defrecord Game [players pieces current-player winner])
;; name is a String.
;; captures is an integer.
(defrecord Player [name captures])
;; player is an index into the Game players.
;; location is a 2 element vector containing x and y coordinates.
(defrecord Piece [player location])

;; Begin defining tools for creating instances of the data types.

(defn- make-player
  "Create a new player with the given name."
  [name]
  (->Player name 0))

(defn- add-player
  "Adds a new player with the given name to the game."
  [game name]
  (update-in game [:players] conj (make-player name)))

(defn- add-players
  "Creates players with the given name and adds them to the game."
  [game player-names]
  (reduce add-player game player-names))

(defn make-game
  "Creates a blank new game"
  []
  (-> (->Game [] [] 0 nil)
      (add-players PLAYER_NAMES)))

;; Begin the locig related to making moves.

(defn- increment-current-player
  "This function just sets the current player to be the next player in the order"
  [{:keys [players current-player] :as game}]
  (let [number-players (count players)
        next-player (mod (inc current-player) number-players)]
    (assoc-in game [:current-player] next-player)))

(defn- occupied?
  "This indicates whether a piece is already at the specified location."
  [{:keys [pieces]} [x y :as location]]
  (->> pieces
       (map :location)
       (some #{location})))

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

(defn- get-capture-locations
  "Returns the CAPTURE_OFFSETS offset for the particular location in question."
  [game location]
  (map (fn [offset-group]
         (map #(mapv + % location) offset-group))
       CAPTURE_OFFSETS))

(defn- get-piece-at-location [{:keys [pieces]} location]
  "Returns the piece at the specified location. nil if there is no piece."
  (->> pieces
       (filter (fn [{:keys [player]
                     piece-location :location}]
                 (= location piece-location)))
       first))

(defn- friendly?
  "Is there a piece friendly to player at location?"
  [{:keys [pieces] :as game} player location]
  (let [piece (get-piece-at-location game location)]
    (= player (:player piece))))

(defn- enemy?
  "Is there an enemy piece of player at location?"
  [{:keys [pieces] :as game} player location]
  (let [piece (get-piece-at-location game location)]
    (and piece
         (not= player (:player piece)))))

(defn- do-captures-for-locations
  "This will attempt to capture any pieces for Player.
  Locations is the list of locations we need to look at.
  The first and last locations should have friendly pieces and the rest should be enemy in order for a capture to succeed."
  [game player locations]
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

(defn- do-captures
  "This will attempt to perform any captures that need to happen for a player who just played at location."
  [game player [x y :as location]] ;; This COULD assume that the player is the current player, but it might not have been incremented yet
  (let [capture-locations (get-capture-locations game location)]
    (reduce #(do-captures-for-locations %1 player %2) game capture-locations)))

;; This section will cover the rules of winning.

(defn- five-captures-winner
  "This function returns a player who has 5 captures."
  [{:keys [players]}]
  (->> players
       (map :captures)
       (map #(<= 5 %))
       (map-indexed (fn [index value]
                      (if value index)))
       (drop-while nil?)
       first))


;; This section will handle the overly complex task of determining whether somebody has 5 in a row.

(defn- make-blank-grid
  "This function will make a size by size grid with all values initialized to nil."
  [size]
  (->> (repeat size nil)
       vec
       (iterate vec)
       (take size)
       vec))

(defn- convert-pieces-to-grid
  "This function converts the pieces to a 2-dimensional vector where the values are the player who has a stone at that location."
  [pieces]
  (let [grid (make-blank-grid BOARD_SIZE)]
    (reduce (fn [grid {:keys [player location]}]
              (assoc-in grid (rseq location) player))
            grid
            pieces)))

(defn- transpose
  "This function transposes a 2-dimensional grid."
  [grid]
  (let [old-width (count (first grid))]
    (reduce (fn [arr old-col]
              (conj arr (mapv #(get % old-col) grid)))
            []
            (range old-width))))

(defn- diagonal
  "This function returns the diagonal of a matrix starting at start-location and going down and towards the right."
  [grid [x y :as start-location]]
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

(defn south-west-diagonals
  "This function will return all the northeast to southwest diagonals in grid."
  [grid]
  (let [x-max (count grid)
        y-max (count (first grid))
        top-starts (map #(vector % 0) (range x-max))
        left-starts (map #(vector 0 %) (range 1 y-max))
        all-starts (concat top-starts left-starts)]
    (map #(diagonal grid %) all-starts)))

(defn south-east-diagonals
  "This function returns all the northwest to southeast diagonals in grid."
  [grid]
  (let [x-max (count (first grid))
        y-max (count grid)
        top-starts (map #(vector % 0) (range x-max))
        right-starts (map #(vector x-max %) (range 1 y-max))]))

(defn get-all-lines
  "This will get all lines for a grid including rows, columns and both sets of diagonals."
  [grid]
  (let [trans (transpose grid)
        sw-diags (south-west-diagonals grid)
        se-diags (south-west-diagonals (mapv (comp vec rseq) trans))]
    (concat grid trans sw-diags se-diags)))

(defn n-consecutive
  "This function will return a value if the value appears n consecutive times in the collection"
  [n coll]
  (loop [[x & xs :as coll] coll
         cur-item nil
         cur-count 0]
    (cond (>= cur-count n) cur-item
          (empty? coll) nil
          (nil? x) (recur xs x cur-count)
          (= x cur-item) (recur xs cur-item (inc cur-count))
          :else (recur xs x 1))))

(defn five-in-a-row-winner
  "This function returns a player who has 5 in a row"
  [{:keys [pieces]}]
  (let [grid (convert-pieces-to-grid pieces)
        lines (get-all-lines grid)]
    (some #(n-consecutive 5 %) lines)))

(defn get-winner
  "This function returns the winner of the game or nil if there is not one."
  [game]
  (or (five-captures-winner game)
      (five-in-a-row-winner game)))

;; Final winner-checking function

(defn- check-winner
  "Checks to see if there is a winner and if so sets the winner in the game."
  [game]
  (if-let [winner (get-winner game)]
    (assoc-in game [:winner] winner)
    game))

;; Validation Functions

(defn- within-bounds?
  "Verify that the location is a valid board location."
  [[x y]]
  (and (<= 0 x (dec BOARD_SIZE))
       (<= 0 y (dec BOARD_SIZE))))

;; Finally what the rest of the file has been building up to, the move function.

(defn move
  "This is the most important function for users outside the namespace to call.
  It tries to place a piece at location."
  [{:keys [current-player winner] :as game} [x y :as location]]
  (cond
   winner game
   ((complement within-bounds?) location) game
   (occupied? game location) game
   :else (-> game
             (update-in [:pieces] conj (->Piece current-player [x y]))
             (do-captures current-player location)
             check-winner
             increment-current-player)))
