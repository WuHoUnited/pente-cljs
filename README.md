pente-cljs
==========

Pente
=====

Pente is a strategy game played on a grid where the goal is to get 5 stones in a row, or capture 5 pairs of enemy stones.
This is an implementation of the game pente in ClojureScript. It allows you to play against yourself in a web browser.

This particular implementation was done using `ClojureScript`, `Om` and `Sablono`.

Quick Start
===========

To begin playing, navigate to the root directory of the project and run
`lein make once prod` which will run a leiningen task to build the production project.

Open the `public/prod/index.html` file.

Developing
==========

This code was developed using `leiningen` and `Light Table`.

To begin developing follow the following steps:

1. Run `lein make auto dev` to start `cljx` and `cljsbuild`.

2. Open up `main.cljs` in `Light Table` and start a browser session.

3. Copy the Script tag that `Light Table` prompts you with and paste it
    into the `public/dev/index.html` file replacing the existing script.

4. Open up the `public/dev/index.html` file in your web browser.

