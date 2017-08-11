FROM clojure:lein-2.7.1-alpine

WORKDIR /app

CMD ["lein", "with-profile", "+etcd", "repl", ":headless" ]
