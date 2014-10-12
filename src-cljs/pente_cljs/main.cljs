(ns pente-cljs.main
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [schema.core :as s :include-macros true]
            [pente.core :as p]

            [clojure.string]
            [sablono.core :as html :refer-macros [html]]))

(enable-console-print!)
#_(s/set-fn-validation! true)

;; I don't know how to do correct scaling of SVG. Have to look into trying to use viewBox
(def SVG_HEIGHT "The total height of the SVG image"
  585)
(def SVG_WIDTH "The total width of the SVG image"
  585)
(def BOARD_SIZE "The size of the board.
  Note that the board size is bigger than the lines.
  This could actually be calculated from the padding and line spacing."
  585)
(def EDGE_PADDING "This is the padding that will exist between the edge of the board
  and the nearest lines."
  20)
(def LINE_SPACING "This is the space between each adjacent line on the board."
  30)
(def STONE_RADIUS "This is the radius of a single stone. You will note that it is less than or equal to the line spacing."
  14)
(def HOSHI-RADIUS "This is the radius of the hoshi points."
  4)
(def LINE_END "This is the coordinate for the end points of the line.
  We're just caching this value here."
  (+ EDGE_PADDING
     (* LINE_SPACING (dec p/BOARD_SIZE))))

(def app-state "This will hold the state of the game."
  (atom (p/make-game)))

(defn move!
  "This will attempt to place a stone at the specified location."
  [app x y]
  (om/transact! app #(p/move % [x y])))

(defn reset-game!
  "This will create a fresh new game."
  [app]
  (om/transact! app (constantly (p/make-game))))

(defn player-view
  "Displays information about a single player."
  [{:keys [name captures] :as player} key is-current]
  (let [current-class (if is-current "current")]
    (html [:li {:className (clojure.string/join " " ["player" current-class])
                :key key}
           [:div {:className "player-name"} name]
           [:div {:className "captures"} captures]])))

(defn players-view
  "This will display the information regarding all players in the game."
  [{:keys [players current-player] :as game}]
  (html [:ul {:className "players"}
         player-view
         (map-indexed (fn [index player]
                        (player-view player index (and (not (:winner game)) (== index current-player))))
                      players)]))

(defn draw-lines
  "This function will draw the horizontal and vertical lines on the board."
  [app]
  (html (mapcat
         (fn [num]
           (let [offset (+ EDGE_PADDING
                           (* num LINE_SPACING))]
             [[:line {:className "line"
                      :x1 EDGE_PADDING
                      :y1 offset
                      :x2 LINE_END
                      :y2 offset
                      :strokeLinecap "square"}]
              ;;Should remove this "almost" duplication
              [:line {:className "line"
                      :y1 EDGE_PADDING
                      :x1 offset
                      :y2 LINE_END
                      :x2 offset
                      :strokeLinecap "square"}]]))
         (range p/BOARD_SIZE))))

(defn draw-hoshi
  "This function will draw the hoshi (stars) on the board."
  [app]
  (let [intersections [3 (quot p/BOARD_SIZE 2) (- p/BOARD_SIZE 4)]]
    (for [row intersections
          col intersections]
      (html [:circle {:className "hoshi"
                      :cx (+ EDGE_PADDING
                             (* LINE_SPACING col))
                      :cy (+ EDGE_PADDING
                             (* LINE_SPACING row))
                      :r HOSHI-RADIUS}]))))

(defn draw-board
  "Displays the board, including the lines.
  Returns a seq of om components."
  [app]
  (html [:rect {:width BOARD_SIZE
                :height BOARD_SIZE
                :className "board"
                :key "board"}]
        (draw-lines app)
        (draw-hoshi app)))

(defn draw-stone
  "This will draw a stone at the specified row and column.
  Config will be options you wish tot merge into the om/react map."
  [x y config]
  (html [:circle (merge {:cx (+ EDGE_PADDING
                                (* x LINE_SPACING))
                         :cy (+ EDGE_PADDING
                                (* y LINE_SPACING))
                         :r STONE_RADIUS}
                        config)]))

(defn get-player-class-name
  "This generates the CSS class name for the given player.
  The different players need to have different classes because their
  stones are different colors."
  [player]
  (str "player-" player))

(defn ghost-stones-view
  "The \"Ghost Stones\" are the stones which display where your mouse is.
  They show the places where you can legally play and also contain the
  click handlers for actually moving.
  Returns a seq of om components."
  [{:keys [pieces current-player] :as app}] ;; this could be used filter out already placed pieces
  (for [x (range p/BOARD_SIZE)
        y (range p/BOARD_SIZE)]
    (draw-stone x y {:className (str "ghost-stone "
                                     (get-player-class-name current-player)) ;;This would be more efficient if done at the board level
                     :onClick #(move! app x y)
                     :key (str "g" x "_" y)})))

(defn pieces-view
  "This displays all the stones. Returns a seq of om components."
  [{:keys [pieces] :as app}]
  (map (fn [{:keys [player] [x y :as location] :location}]
         (draw-stone x y {:className (get-player-class-name player)
                          :key (str x "_" y)}))
       pieces))

(defn board-view
  "This contains the view of the entire board including the pieces."
  [app]
  (html [:svg {:className "game-image"
               :height SVG_HEIGHT
               :width SVG_WIDTH}
         (draw-board app)
         (if-not (:winner app)
           (ghost-stones-view app))
         (pieces-view app)]))

(defn command-view
  "This will generate the list of commands."
  [app]
  (html [:ul {:className "commands"}
         [:li [:button {:onClick #(reset-game! app)}
               "Reset Game"]]]))

(defn winner-view [app]
  (if-let [winner (:winner app)]
    (let [winner-name (get-in app [:players winner :name])]
      (html [:div {:className "winner"}
             (str  winner-name " wins!")]))))

(defn game-view
  "This displays the entire application including players, board and pieces."
  [app owner]
  (reify om/IRender
    (render [_]
            (html [:div
                   (board-view app)
                   [:div {:className "right-holder"} ;; I'm not good enough with CSS to avoid this div
                    (command-view app)
                    (winner-view app)
                    (players-view app)]]))))

;; This piece is for trying out undo functionality.

(def app-history (atom [@app-state]))

(add-watch app-state :history
           (fn [_ _ _ n]
             (when-not (= (last @app-history) n)
               (swap! app-history conj n))))

(aset js/window "undo"
      (fn [e]
        (when (> (count @app-history) 1)
          (swap! app-history pop)
          (reset! app-state (last @app-history)))))

;; Begin rendering to the DOM.
(om/root
 game-view
 app-state
 {:target (. js/document (getElementById "app"))})

(print-str @app-state)

(reset! app-state
        (reduce
         p/move
         (p/make-game)
         [[0 1]
          [0 0]
          [0 3]
          [0 4]
          [8 8]]))


