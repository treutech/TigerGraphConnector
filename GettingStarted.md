
# Getting Started

This guide will provide the steps necessary to configure and start the source and sink connectors to work with TigerGraph. It also provides steps to install and configure the TigerGraph Docker container to be used for basic testing of the the source and sink connectors.

---

## Table of Contents


- [Getting Started](#getting-started)
	- [Table of Contents](#table-of-contents)
    - [Quick Start](#quick-start)
		- [Requirements](#requirements)
		- [Setting up TigerGraph](#setting-up-tigergraph)
            - [Introduction to TigerGraph](#introduction-to-tigergraph)
            - [Setup](#setup)
			    - [Start the TigerGraph Container](#start-the-tigergraph-container)
                - [Load Schema into TigerGraph](#load-schema-into-tigergraph)
                - [Load Sample Data into TigerGraph](#load-sample-data-into-tigergraph)
                - [Additional TigerGraph Operations](#additional-tigergraph-operations)
            - [Setting Up the TigerGraph Kafka Connector](#setting-up-the-tigergraph-kafka-connector)
                - [Generate the Connector from the sources](#generate-the-connector-from-sources)
                - [TigerGraph Connector Manual Installation](#tigergraph-connector-manual-installation)
                - [Running the TigerGraph Connector](#running-the-tigergraph-connector)
        - [General Notes on Configuring Connectors for use against your own code](#general-notes-on-configuring-connectors-for-use-against-your-own-code)



## Quick Start

### Requirements

- Docker Desktop
- Kafka ```(Note: the latest version of the connector was tested with Confluent CE 7)```. This document does not cover how to set up the Confluent Platform. It assumes that an existing instance of Kafka is running and available. This includes:
    - at least one Broker instance
    - Schema Registry `(Note: The Schema Registry IS REQUIRED to be running in order to use the TigerGraph connector using the AVRO message format. The necessary schema will be added automatically by the connector. And it is required for testing in this Quick Start guide, as AVRO is used.)`
    - Start the Confluent platform with the command: ```confluent local services start``` To test the connector, it is necessary to stop the Kafka Connect Service: ```confluent local services connect stop```
- Java (version 8 at least)

### Setting Up TigerGraph

#### Introduction to TigerGraph

TigerGraph is a graph based database. It works by having multiple vertices connected by multiple edges. A schema is the structure of your graph. It is depicted by different vertex types being connected together by different edge types. Once a schema has been added, the Sink Connector can start ingesting data. To query this data in your graph, you need to develop queries using TigerGraph's GSQL. These queries can be installed into your graph either by a command line interface or through TigerGraph's GraphStudio (a visual and interactive tool for managing your graphs). Once queries are installed, the Source Connector can be configured to run one of the preinstalled queries.


#### Setup

The root directory of this project will be referred as the `TigerGraphConnector folder`.

In order for the connector to work (source and sink), an instance of TigerGraph is needed to be running on your machine with a Schema and a pre-installed query.

For your own environment and development purposes you will need to define the query to be used for the connector (This requires fundamental knowledge about TigerGraph and GSQL which is beyond the scope of this documentation). For demonstration purposes, an example query is provided as part of this quick start to demonstrate how to do so and to facilitate initial testing of the connector with TigerGraph.

---

##### Start the TigerGraph Container

1) Pull the official TigerGraph Developer Docker Image by running:
   `docker pull docker.tigergraph.com/tigergraph-dev:latest`

2) Start the docker image by running:

   ```docker run -d -p 14022:22 -p 9000:9000 -p 14240:14240 --name tigergraph_dev --ulimit nofile=1000000:1000000 -v ~/data:/home/tigergraph/data -t docker.tigergraph.com/tigergraph-dev:latest```

Once started, you can stop the TG docker container with the following command:
    `docker container stop tigergraph_dev`

To start the container after it has been stopped, use the following command: `docker container start tigergraph_dev`


3) Once the TG docker container is started, SSH into the docker container with:

   `ssh -p 14022 tigergraph@localhost`

   The password is `tigergraph`

4) Inside the TigerGraph shell, start TigerGraph by running:

   `gadmin start all` (*Note: If you stop and restart the container, you will need to shell into the container and issue the `gadmin start all` command.*)

There is a command that will need to be run just once to optimize the use of the developer version of TigerGraph on your machine, especially if you are running Kafka on the same hardware.

- `Set the REST timeout`: When running TigerGraph with other services on a dev platform, queries can time out prematurely. Do the following to increase the REST timeout period:

If a docker shell is not already open, open a terminal and execute the shell command `ssh -p 14022 tigergraph@localhost`

At the command line execute the following command: `gadmin config entry RESTPP.Factory.DefaultQueryTimeoutSec`

Change the default timeout from 16 seconds to 120 seconds and press enter.

Execute the command `gadmin config apply` and select (Y)es to apply the updated configuration.

Execute the command `gadmin restart` and select (Y)es to restart the TigerGraph instance and bind the new setting into your runtime environment.

Also ensure that your Docker Desktop is configured to allow for at least 4 GB of RAM (8 GB of RAM is recommended) for running Containers, as TigerGraph is a resource hoarder and operates better with plenty of operational RAM. This is done via preferences and from the resources tab inside of Docker Desktop.

---

##### Load Schema into TigerGraph

Once TigerGraph is configured and running, open a browser and access the [GraphStudio](http://localhost:14240/#/home) via `http://localhost:14240/#/home` and you should have access to TigerGraph via the TigerGraph Graph Studio. Currently there is no graph created so you will be in `Global View`. The menus of interest are:

- `Design Schema` to see the schema of a graph, which is a graph of types - similar in concept to the schema of relational database.
- `Explore Graph` to visualize parts of the graph.
- `Write Queries` to edit, create and execute graph queries.

Next, Install a schema and queries in TigerGraph. There are two ways of installing a schema, either by going to the [GraphStudio](http://localhost:14240/#/home) web portal or, by running `gsql` commands. There exists an example schema called `CountryCitySchema.gsql` in the `example` folder which contains a schema creation. This can be executed via `gsql` to install the schema, this is recommended for the initial testing of the TigerGraph Connector. To do so, execute the following lines within a command shell in the TigerGraph Docker Container.

1) From a terminal, copy files `CountryCitySchema.gsql`, `clear_all.gsql`, `clear_store.gsql`, `mygraph_job.gsql` into your TigerGraph docker container home directory, using the following docker commands:

   `docker cp example/CountryCitySchema.gsql tigergraph_dev:/home/tigergraph`

   `docker cp example/clear_all.gsql tigergraph_dev:/home/tigergraph`

   `docker cp example/clear_store.gsql tigergraph_dev:/home/tigergraph`

   `docker cp example/mygraph_job.gsql tigergraph_dev:/home/tigergraph`

2) In the TigerGraph shell instance, update the owner and group for these files:

   `sudo chown tigergraph *.gsql`

   `sudo chgrp tigergraph *.gsql`

3)  Then load the schema by running the following gsql command:

   `more CountryCitySchema.gsql | gsql`

You should see output similar to this:

```
~$ more CountryCitySchema.gsql | gsql
Welcome to TigerGraph Developer Edition, free for non-production, research, or educational use.
GSQL-Dev > CREATE VERTEX country ( PRIMARY_ID name STRING, name STRING, number INT )
The vertex type country is created.
GSQL-Dev > CREATE VERTEX city ( PRIMARY_ID name STRING, name STRING, number INT, timestamp_ DATETIME )
The vertex type city is created.
GSQL-Dev > CREATE UNDIRECTED EDGE connection (FROM country, TO city)
The edge type connection is created.
GSQL-Dev > CREATE GRAPH MyGraph(*)
Stopping GPE GSE RESTPP
Successfully stopped GPE GSE RESTPP in 0.070 seconds
Starting GPE GSE RESTPP
Successfully started GPE GSE RESTPP in 0.084 seconds
The graph MyGraph is created.
GSQL-Dev > CREATE QUERY myQuery(SET<vertex<country>> nodes, DATETIME t) FOR GRAPH MyGraph { Start = nodes; R1 = SELECT P FROM Start:s-(connection:rat)-city:P WHERE datetime_to_epoch(P.timestamp_) > datetime_to_epoch(t); PRINT R1;}
The query myQuery has been added!
GSQL-Dev > CREATE QUERY ResetTimeStamp(SET<vertex<country>> nodes) FOR GRAPH MyGraph { Start = nodes; R1 = SELECT P FROM Start:s-(connection:rat)-city:P; UPDATE p FROM R1:p SET p.timestamp_=to_datetime("1970-01-01"); PRINT R1;}
The query ResetTimeStamp has been added!
GSQL-Dev > INSTALL QUERY myQuery, ResetTimeStamp
Start installing queries, about 1 minute ...
myQuery query: curl -X GET 'http://127.0.0.1:9000/query/MyGraph/myQuery?nodes=VALUE&t=VALUE'. Add -H "Authorization: Bearer TOKEN" if authentication is enabled.
ResetTimeStamp query: curl -X GET 'http://127.0.0.1:9000/query/MyGraph/ResetTimeStamp?nodes=VALUE'. Add -H "Authorization: Bearer TOKEN" if authentication is enabled.

[========================================================================================================] 100% (2/2)
```

Note: Sometimes when running the TigerGraph dev container the installation of the queries may fail. This will be indicated by an error message stating such. If this occurs, you can install the queries via the Graph Studio. To do so, select MyGraph to make it the current schema reference and navigate to the `Write Queries` menu option. At the top of the screen near the GSQL Queries text is an UP Arrow icon. If the queries are not installed it will be highlighted, and the tool tip text will say `Install All Queries`. Just click this icon to finalize the step of query installation.

In order for the connector to work properly, the referenced queries in the connector configuration MUST be installed.

Reload Graph Studio in your browser and check the menus mentioned above. You should now see under the `Global View` another view for graph `MyGraph`. If you select that, then you will be able to browse the schema of the graph. The graph (database) is created but it is empty. Next, some sample data needs to be loaded into the graph.

---

##### Load Sample Data into TigerGraph

1) Inside the TigerGraph docker container, within the `home/tigergraph` directory you should have a folder
   called `data`. If not, then create one.

2) Copy the files `country.csv`, `city.csv` and `connection.csv` to that data folder:

   `docker cp example/country.csv tigergraph_dev:/home/tigergraph/data`

   `docker cp example/city.csv tigergraph_dev:/home/tigergraph/data`

   `docker cp example/connection.csv tigergraph_dev:/home/tigergraph/data`

3) Then update the owner and group for these files:

   `sudo chown tigergraph *.csv`

   `sudo chgrp tigergraph *.csv`

4) Now we can populate the graph (database) using a loading job, which uses the three csv files. Execute the
   following command to run the loading job in the TigerGraph command shell:

   `more mygraph_job.gsql | gsql`

Upon completion of that command you should see output similar to the following in the command shell:

```
~$ more mygraph_job.gsql | gsql
Welcome to TigerGraph Developer Edition, free for non-production, research, or educational use.
GSQL-Dev > USE GRAPH MyGraph
Using graph 'MyGraph'
GSQL-Dev > CREATE LOADING JOB mygraph_job FOR GRAPH MyGraph { DEFINE FILENAME country_file = "/home/tigergraph/data/country.csv"; DEFINE FILENAME city_file = "/home/tigergraph/data/city.csv"; DEFINE FILENAME connection_file = "/home/tigergraph/data/connection.csv"; LOAD country_file TO VERTEX country VALUES ($0, $1, $2) USING header="true"; LOAD city_file TO VERTEX city VALUES ($0, $1, $2, $3) USING header="true", QUOTE="double"; LOAD connection_file TO EDGE connection VALUES ($0, $1) USING header="true"; }
Semantic Check Fails: The job name mygraph_job already exists in other objects!
The job mygraph_job could not be created!
GSQL-Dev > RUN LOADING JOB mygraph_job
[Tip: Use "CTRL + C" to stop displaying the loading status update, then use "SHOW LOADING STATUS jobid" to track the loading progress again]
[Tip: Manage loading jobs with "ABORT/RESUME LOADING JOB jobid"]
Starting the following job, i.e.
  JobName: mygraph_job, jobid: MyGraph.mygraph_job.file.m1.1629752555660
  Loading log: '/home/tigergraph/tigergraph/log/restpp/restpp_loader_logs/MyGraph/MyGraph.mygraph_job.file.m1.1629752555660.log'

Job "MyGraph.mygraph_job.file.m1.1629752555660" loading status
[FINISHED] m1 ( Finished: 3 / Total: 3 )
  [LOADED]
  +--------------------------------------------------------------------------------+
  |                            FILENAME |   LOADED LINES |   AVG SPEED |   DURATION|
  |      /home/tigergraph/data/city.csv |             57 |      56 l/s |     1.00 s|
  |/home/tigergraph/data/connection.csv |             57 |      56 l/s |     1.00 s|
  |   /home/tigergraph/data/country.csv |             12 |      12 l/s |     1.00 s|
  +--------------------------------------------------------------------------------+
```


Now access the `Explore Graph` menu in the Graph Studio (ensure `MyGraph` has been selected in the upper left corner of the screen), and click on `Pick Vertices`. The graph display should show 5 of each type of vertex, for `country` and `city`.


Also, in the `Write Queries` menu, by executing the `myQuery` query and passing `country` node ids to the first parameter (a set of ids) and a timestamp for the last parameter (this may be empty), you can list the available `country` nodes and browse additional connected nodes by double-clicking on them.

---

#### Additional TigerGraph Operations

During testing of the TigerGraph connector, it may be desirable to reload data for testing. Following are a couple of GSQL scripts for doing so.

- To regenerate the graph data execute the following steps:

		a) Run generator
		b) Copy new files to docker data folder overwriting any older files
		c) If data was previously loaded, execute a clear store (*mentioned below*)
		d) Rerun the loading job with the command `more mygraph_job.gsql | gsql`


- Clear up and restart. If for any reason, you need to delete the graph (database) you have two options. One option is complete deletion, which means that all graph content, graph schema, and queries are removed from the TigerGraph instance. For this case, use the `clear_all.gsql` script:

    `more clear_all.gsql | gsql`

- Data deletion, which means you delete only graph content (schema and queries remain untouched). This option is useful to repopulate the graph. For this case, use the `clear_store.gsql` script.

    `more clear_store.gsql | gsql`

### Setting Up the TigerGraph Kafka Connector

It is presumed in this guide that you already have a running instance of Kafka along with the Schema Registry available on your computer or accessible within your operating environment. If you don't have the Schema Registry running, start it up. Documentation for doing so can be found here: [Schema Registry Documentation](https://docs.confluent.io/platform/current/schema-registry/index.html)

To run the connectors using the provided sample schema and data, a topic named `tigergraph` must exist.

If you have `auto.create.topics.enable` enabled in your broker server properties configuration you don't need to manually create the topic. Otherwise create the `tigergraph` topic manually.



#### Generate the Connector from the sources

1) To generate the connector exportable from the sources you need Java 8 (or above) and Maven installed in your system.

2) From the command line go to the `lib` directory of this project.

3) Add the TigerGraph JDBC Driver to your local Maven repository, with the command:

```
mvn install:install-file \
   -Dfile=tg-jdbc-driver-1.2.jar \
   -DgroupId=com.tigergraph.tg-jdbc-driver \
   -DartifactId=tigergraph-driver \
   -Dversion=1.2 \
   -Dpackaging=jar \
   -DgeneratePom=true
```

4) From the root of the project type `mvn clean package`

5) This command will generate the file:  `TigerGraphConnector-1.0.0-with-all-dependencies.jar` inside the target directory.


#### TigerGraph Connector Manual Installation

1) Locate the confluent-X.X.X directory on your system. This will be referred to as the `<path-to-confluent>` from here on.

2) Create a `tmp` directory in the root of the confluent directory. The path to the `tg-connect.offsets` file should look like this: `<path-to-confluent>/tmp/tg-connect.offsets`. The SourceTask will publish offsets to this file. The Source Connector will create this file if it does not exist. It is only necessary that the tmp directory is created.

3) Next, create a folder named `kafka-connect-tigergraph` in the `<path-to-confluent>/share/java` directory.

4) From the `dist` directory (or from `target` directory, if you generated it), copy the following file into `<path-to-confluent>/share/java/kafka-connect-tigergraph`:

    `TigerGraphConnector-1.0.0-with-all-dependencies.jar`

5) Create a directory under `<path-to-confluent>` named config/tigergraph where the fully qualified path to this directory would be `<path-to-confluent>/config/tigergraph`.

6) Provided in the `etc` folder are configuration files for both the Sink and Source Connectors: copy the `connect.properties`, `tigergraph.sink.properties` and `tigergraph.source.properties` files to the `<path-to-confluent>/config/tigergraph` directory.

7) Change the `<path-to-confluent>` to your Confluent Platform directory. Edit the `<path-to-confluent>/config/connect.properties` file and specify the absolute path to the `tg-connect.offsets` file:

`offset.storage.file.filename=<path-to-confluent>/tmp/tg-connect.offsets`

and specify the plugin path:

`plugin.path=<path-to-confluent>/share/java`

If you are running Kafka on a separate host, modify the bootstrap servers list in the connect.properties file accordingly.

`bootstrap.servers=<your-kafka-host-name-or-IP>:9092`

The `tigergraph.sink.properties` and the `tigergraph.source.properties` files are already configured to work with the sample `MyGraph` configuration and sample data provided for this Quick Start guide.

#### Running the TigerGraph Connector

You are ready to send messages to TigerGraph using the connector! For the Sink task to work data needs to reside in the topic. Using the facility provided in this Quick Start Guide the source task must be run first to add data to the test tigergraph topic from TigerGraph. Once data resides in the tigergraph topic then the sink task can be started up to push data from the tigergraph test topic back into TigerGraph.

To test the source and sink connectors, do the following steps.

Navigate to your `<path-to-confluent>/bin` directory and execute the following command to run the Source Connector Task:

**Source Connector:** `./connect-standalone ../config/tiger-graph/connect.properties ../config/tiger-graph/tigergraph.source.properties ../config/tiger-graph/tigergraph.sink.properties`

Once it is started you should see the poll info log lines in your console display. It may take a few moments before you start seeing messages being posted to the topic tigergraph in Kafka.

You can verify that the `tigergraph` topic exists with the following command executed in the bin directory of your Kafka installation:

`./kafka-topics --list --bootstrap-server localhost:9092`

Additionally, if you want to verify messages are being posted to the `tigergraph` topic, execute the following command:

`./kafka-avro-console-consumer --group tigergraph --from-beginning --topic tigergraph --bootstrap-server localhost:9092`


Let the source connector run for a while to allow for a good amount of messages to flow into Kafka. Then stop the source connector. While running, will see occasional log messages on the console with non-zero message counts being flushed. This is another way to verify that the messages are flowing from TigerGraph to Kafka.

There are two approaches you can take to facilitate testing the Sink Connector. One approach is highlighted at this point, and the latter one will be detailed after testing with the Sink Connector accordingly.

In the Graph Studio in your browser ([http://localhost:14240/#/home](http://localhost:14240/#/home)) click on the `Write Queries` tab. (Be sure to have `MyGraph` selected in the upper left corner of the page.) Then click on `RestTimeStamp`.

Click on the `Run Query` icon and a dialog will open up. Click on the `+` symbol, ensure the `country` type is selected, then add a vertex id of your choosing and click on the `Run Query` button.

The sample graph data is added in sequential format starting at 1, so you can start with 1, then repeat this step for 2, 3 etc. as many `country` vertexes you wish to test for.

Once you have updated a handful of `country` nodes then click on the `Explore Graph` tab. Using the first option of `Search vertices by vertex id`, select the `country` type, and then enter an ID from the list of `country` Ids you ran the reset time stamp query for. As they show up in the graph viewer, double-click on the found `country` vertex and then double-click on one of the connected `city` vertices. The tool-tip info should now show the `unix epoch time` for that node.

(*Hint: You can clear the Graph Viewer of nodes by clicking on the undo icon.*)

Sample a few of the updated `country` vertex nodes to your satisfaction. Now you are ready to run the Sink Connector.

Navigate to your `<path-to-confluent>/bin` directory and execute the following command to run the Sink Connector Task:

**Sink Connector:** `./connect-standalone ../config/tiger-graph/connect.properties ../config/tiger-graph/tigergraph.sink.properties`

Once started up let the Sink Connector run for a while.

If you are unsure if the sink connector is running, you can change the log level of the `<path-to-confluent>/etc/kafka/connect-log4j.properties` file to `DEBUG`. You will see a lot of log messages when doing this, but you will see log messages indicating that messages are being posted to TigerGraph when it is working properly.

Once it has been running for a while, you can go back to the Graph Studio and follow the steps aforementioned for sampling `country` nodes. Re-sample the nodes you searched for before, and you should see new `city` vertex nodes with non-epoch time stamps. Once you start seeing these new nodes, this is how you know the Sink Connector is working.


(*Please note that the logs of both tasks will dump into the `out.log` file.*)

---

Another quick approach to verify that data is being posted to TigerGraph from the tigergraph topic is to do the following:

With data having been loaded via the Source Connector, open up a terminal session and SSH into the TigerGraph container:

`ssh -p 14022 tigergraph@localhost`

From the command prompt execute the following command:

`more clear_store.gsql | gsql`

That will clear the `MyGraph` instance of all data while leaving the schema intact.

Then run the Sink Connector. You will see new `city` vertex nodes appear as they get loaded, but they will not connect to anything as the only data that is stored in the tigergraph topic are `city` vertex nodes, and by running the `clear_store.gsql` script, all of the `country` vertex nodes and connecting edges data has been cleared. But at least you can see data is getting loaded into the graph without having to search for them.


---

### General Notes on Configuring Connectors for use against your own code

When using the TigerGraph Source Connector for real-world use cases, the Source Connector relies on queries being loaded into TigerGraph before you can execute them. It is not possible to write ad-hoc queries and just plug them into the connector configuration as for SQL databases. You will have to write your own GSQL queries and install them first.

Once installed you then reference the query using the `tigergraph.source.query` property (see the provided example `tigergraph.source.properties` configuration properties file as a reference). The TigerGraph Source connector uses the TigerGraph JDBC driver, hence it uses database metadata to parse the relevant data to post to Kafka. The graph data is referenced via a virtualized columnar format, and to select which columnar data to select from, the `tigergraph.source.args` property defines which graph elements to parse into fields for the schema that is generated (when using AVRO) that get posted to Kafka, as if they were columns. So in essence it is similar to how normal JDBC columns are referenced. The order of elements is dependent on the result returned by the GSQL query that you provide to the configuration.

The property `tigergraph.source.query.pattern` defines the TigerGraph based pattern that goes in hand with the query to parse the graph data into relative field references to establish distinct messages for each set of data returned by the GSQL query, which subsequently is posted to the Kafka topic defined in the task configuration.

TigerGraph has no notion of timestamp columns per se, as they are defined for SQL databases. To effect Timestamp polling in the TigerGraph connector, set the `tigergraph.source.timestamp.enabled` to `true`, and define a property in the vertex of your graph with the name corresponding to the `tigergraph.source.timestamp.attributeName` value defined in your Source Task configuration.

Using this approach is the only means at present to query data from a graph in an orderly sequential format. When the `tigergraph.source.timestamp.enabled` is set to `false`, data that is retrieved from TigerGraph which gets posted to your Kafka topic is wholly dependent on the GSQL query that you provide it. And it may provide duplicate data depending on the nature of the data in your graph, and will not be ordered. In other words, there is no *sequence* based approach like for SQL databases where an auto-incrementing sequence key is used which correlates to how GSQL works with TigerGraph in this respect.

If you need something like this in your use case, it is up to you to define how that will work utilizing attributes in your graph data and manage it using GSQL. And even so, depending on the query you provide, it may not provide data in an ordered format in the same manner you would expect from a SQL database. It all depends on the data in your graph and how you write your GSQL query.

When setting up TigerGraph for your development use case, be sure to allocate appropriate resources for TigerGraph. The primary resource bing RAM, be sure to specify at least 4 GB for dev concerns, sometimes more like 8 GB is recommended. Here is a link to recommended resource requirements for TigerGraph. [https://docs.tigergraph.com/admin/admin-guide/hw-and-sw-requirements](#https://docs.tigergraph.com/admin/admin-guide/hw-and-sw-requirements)



