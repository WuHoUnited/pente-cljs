pente-cljs
==========

Pente
=====

Pente is a strategy game played on a grid where the goal is to get 5 stones in a row, or capture 5 pairs of enemy stones.
This is an implementation of the game pente in ClojureScript. It allows you to play against yourself in a web browser.

This particular implementation was done using `ClojureScript`, `Om` and `Soblono`.

Quick Start
===========

To immediately begin playing open the `public/prod/index.html` file.

Developing
==========

This code was developed using `leiningen` and `Light Table`.

To begin developing follow the following steps

1. Start `cljx` processing and moving files from the `src-cljx` folder to the `src-cljs` directory.
    
    `lein cljx auto`

2. Begin automatically building the clojurescript files

    `lein cljsbuild auto dev`

3. Open up `main.cljs` in `Light Table` and start a browser session

4. Copy the Script tag that `Light Table` prompts you with and paste it 
    into the `public/dev/index.html` file replacing the existing script

5. Open up the `public/dev/index.html` file in your web browser.
