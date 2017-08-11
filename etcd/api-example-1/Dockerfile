FROM clojure:lein-2.7.1-alpine

COPY project.clj /download-deps/project.clj
WORKDIR /download-deps
RUN lein deps
ADD . /app

WORKDIR /app
CMD ["lein", "run"]
