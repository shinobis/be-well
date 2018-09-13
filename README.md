# Be-Well

A sample cordapp to demonstrate the use of queryable states in
corda through the use-case of a health and wellness application.

**Be-well** represents a network with two kinds of participants -
**user brokers** and **wellness providers**. User brokers can be
any organization that offers to manage user information and gives them
access to the network. Wellness providers may range from fitness and
sports centers to health clinics that are willing to work with clients
and their health.
The environment may be visualized with various brokers interacting with
different wellness providers to offer services to clients. This
application presents a simplified version of this with three features -
creation of wellness records, updates and wellness score metrics.

The contracts module defines the wellness contract that contains the
attributes, commands and corresponding verification for transactions.
Three flows defined in the cordapp module capture the features mentioned
above - create, update and wellness scores. Additionally the cordapp
also includes a basic web api to interact with a sample deployment.

## Pre-Requisites

You will need the following installed on your machine before you can start:

* [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
  installed and available on your path (Minimum version: 1.8_131).
* [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (Minimum version 2017.1)
* git
* Optional: [h2 web console](http://www.h2database.com/html/download.html)
  (download the "platform-independent zip")

For IDE, compilation and JVM version issues, see the
[Troubleshooting](https://docs.corda.net/troubleshooting.html) page on the Corda docsite.

## Getting Set Up

To get started, clone this repository with:

     git clone https://github.com/shinobis/be-well.git

And change directories to the newly cloned repo:

     cd be-well

## Flows

* **Create Wellness Flow:** creates a new wellness state for a single user.
* **Update Wellness Flow:** to update the wellness details of a user.
* **Score Wellness Flow:** retrieves consumed and current states to generate a wellness score.


## Building the Cordapp:

**Unix:** 

     ./gradlew deployNodes

**Windows:**

     gradlew.bat deployNodes

Note: You'll need to re-run this build step after making any changes to
the cordapps for these to take effect on the node.

## Running the Nodes

Once the build finishes, change directories to the folder where the newly
built nodes are located:

     cd build/nodes

The Gradle build script will have created a folder for each node. You'll
see three folders, one for each node and a `runnodes` script. You can
run the nodes with:

**Unix:**

     ./runnodes --log-to-console --logging-level=DEBUG

**Windows:**

    runnodes.bat --log-to-console --logging-level=DEBUG

You can also start each node individually by changing into the node's
directory and running the corda jar.

    java -jar corda.jar

You should now have three Corda nodes running on your machine serving
the cordapps.

When the nodes have booted up, you should see a message like the following
in the console:

     Node started up and registered in 5.007 sec

## Running the web servers:

The `runnodes` script starts up the webservers for each node. However, to
individually start up the web server, run the web server jar from the
node directory.

    java -jar corda-webserver.jar

## Interacting with the nodes
Use a tool such as [Postman](https://www.getpostman.com/apps) or curl
(command line), to view data as well as run the various flows.

You can check the identity of node for each webserver using the
`identity` endpoint.

    curl http://localhost:10007/api/wellness/identity


To see available states in the node's vault use:

    curl http://localhost:10007/api/wellness/vault

Here are sample commands for the three flows:
#### Create Wellness Flow

**Unix:**

    curl --header "Content-Type: application/json" --request POST --data '{ "provider" : "First Wellness", "wellnessData" : { "sex" : "male", "age" : "28" } }' http://localhost:10007/api/wellness/create-wellness

**Windows:**

    curl --header "Content-Type: application/json" --request POST --data "{ \"provider\" : \"First Wellness\", \"wellnessData\" : { \"sex\" : \"male\", \"age\" : \"28\" } }" http://localhost:10007/api/wellness/create-wellness

 You should see a message similar to one below:

    Added a new wellness report for account id 23562a68-51ba-412d-bd89-82c0f1405e1c in transaction 49A0179AC27B5F898B5A4AFBD71D4FDAB6D2C970B58B9B92D7A92DB60823D435 committed to ledger.

#### Update Wellness Flow

**Unix:**

    curl --header "Content-Type: application/json" --request POST --data '{ "accountId" : "23562a68-51ba-412d-bd89-82c0f1405e1c", "wellnessData" : { "sex" : "male", "age" : "28", "height" : "168", "weight" : "82", "heartRate" : "60" } }' http://localhost:10007/api/wellness/update-wellness

**Windows:**

    curl --header "Content-Type: application/json" --request POST --data "{ \"accountId\" : \"23562a68-51ba-412d-bd89-82c0f1405e1c\", \"wellnessData\" : { \"sex\" : \"male\", \"age\" : \"28\", \"height\" : \"168\", \"weight\" : \"82\", \"heartRate\" : \"60\"} }" http://localhost:10007/api/wellness/update-wellness

 You should see a message similar to one below:

    Updated wellness report for account id 23562a68-51ba-412d-bd89-82c0f1405e1c in transaction 1F3B6540CF1851E44C04BE094AC8CBDF6A64685E3E76E74028D9D90172E15AC5 committed to ledger.

#### Score Wellness Flow

**Unix:**

    curl --header "Content-Type: text/plain" --request POST --data 23562a68-51ba-412d-bd89-82c0f1405e1c http://localhost:10007/api/wellness/score-wellness

**Windows:**

    curl --header "Content-Type: text/plain" --request POST --data 23562a68-51ba-412d-bd89-82c0f1405e1c http://localhost:10007/api/wellness/score-wellness

 You should see a message similar to one below:

    Added new score for account id 23562a68-51ba-412d-bd89-82c0f1405e1c in transaction A3F5303CB126AC079A6AEA98AD82E14F36C2B60A494CB6534BD2B4366C38EAF0 committed to ledger.

Suggestions and feedback are welcome.