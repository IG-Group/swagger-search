# Swagger Search

[![Clojars Project](https://img.shields.io/clojars/v/ig/swagger-search.svg)](https://clojars.org/ig/swagger-search)

Swagger Search is an application that aggregates the [Swagger](https://swagger.io/)/[OpenAPI](https://www.openapis.org/) 
documentation of your microservice architecture in one place, where your can browse it or search endpoints based on 
their url, documentation, the parameters it accepts or their types.

Swagger Search also provides a Swagger UI for those microservices that haven't their own Swagger UI embedded.

## Features

Integrated with [Etcd](https://github.com/coreos/etcd) and [Consul](https://www.consul.io/) for service discovery, or 
your can plug your own.

The search is powered by [Lucene](https://lucene.apache.org/core/), so you can use the [Lucene's query syntax](http://lucene.apache.org/core/2_9_4/queryparsersyntax.html#Terms).
An English analyzer is used.

The available fields are:

1. service-name: the name specified on the info->title of the swagger doc or the basePath
1. path: of the endpoint
1. method: GET/POST/...
1. summary-and-description 
1. parameters: name of the query/form/path/header parameter, or in the case of a body param, the name of any of the properties of the object or nested objects. 
1. responses: the name of any of the properties of the object or nested objects of any of the responses 
1. types: name of any $ref in parameters or responses

If no field is specified, all of them will be used.

## Run it

First you need to create a configuration file. Look at the [example](/example/swagger.config.edn), which contains all the 
configuration options.

#### Docker

Copy or make the swagger.config.edn avaible in */config* inside the container, something like:

```
docker run -p 7878:3000 -v `pwd`/examples:/config danlebrero/swagger-search
```

Open a browser at http://localhost:7878/

See [DockerHub](https://hub.docker.com/r/danlebrero/swagger-search/) for the latest version.

#### Uberjar

Instead of Docker, you can just run the application manually as a JVM application:

1. Install Java 8
1. Download the [latest release jar](releases)
1. Specify the *SWAGGER_HOME* directory as a environment variable 
1. Place the configuration file in the *SWAGGER_HOME* directory. It must be called *swagger.config.edn*
1. Run:
```
java -jar swaggersearch-vXXXX.jar
```

Open a browser at http://localhost:$whatever-port-you-specified-in-the-config-file/

## Custom service discovery

Swagger Search needs to find out what services you are running. The built-in options are:

1. Hardcoded list
1. Read a local file
1. Read a url
1. Etcd
1. Consul

The configuration example file has more details about each of them.

It is quite possible that none of these suits you, in which case next are the possible options to user your own 
service discovery

#### I dont know Clojure!

Your best option is to learn Clojure. It will do you a lot of good :stuck_out_tongue_winking_eye:.

Failing that, use the *:uri-or-file* provider and have a process that updates the file or have a HTTP server that is
able to return the list of services.

#### Plugin a new provider

To create your own service discovery mechanism:

1. Create a function like the [Etcd one](src/com/ig/swagger/search/discovery/providers/etcd.clj)
1. Package it
1. Either add the package to the classpath or copy it to $SWAGGER_HOME/libs
1. Add any additional jar dependencies to the classpath or to $SWAGGER_HOME/libs

#### Swagger Search as a library

If you don't want to run Docker or Uberjar, Swagger Search is also available as a library 
in [Clojars](https://clojars.org/ig/swagger-search).

See the [uberjar code](standalone/com/ig/swagger/search/standalone.clj) as guide about how you can use it as a library. 

## Known issues

1. No support for OpenAPI 3.0.
1. Swagger 1.2 just indexes the url.
1. Not tested with Swagger < 1.2.
1. Index is stored in memory. 

## Development

Go to examples/etcd and run:

```
docker-compose up
```

This should start a whole developer environment, including an application with a sample Swagger API.

Swagger Search will be available at [http://localhost:8504/](http://localhost:8504/) and a Clojure REPL at localhost:8503

## License

Copyright © 2016 IG Group

Distributed under Apache Licence 2.0.