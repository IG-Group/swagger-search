FROM clojure:lein-2.7.1-alpine

WORKDIR /app

CMD ["lein", "with-profile", "+not-lib", "do", "clean,", "repl", ":headless" ]
