# Be-Well

A sample cordapp to demonstrate the use of queryable states in
corda through the use-case of a health and wellness application.

**Be-well** represents a network with two kinds of participants -
**user brokers** and **wellness providers**. User brokers could represent
any organization that offers to manage users allowing them access to the
network. Wellness providers may range from fitness and sports centers to
health clinics that are willing to work with clients and their health.
The environment may be envisioned with various brokers interacting with
different wellness providers to offer features to clients. This
application presents a simplified version of this with three features -
creation of wellness records, updates and wellness score metrics.

The contracts module defines the wellness contract that contains the
attributes, commands and corresponding verification for transactions.
Three flows defined in the cordapp module capture the features mentioned
above - create, update and wellness scores.

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
the template for these to take effect on the node.

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

You should now have three Corda nodes running on your machine serving 
the cordapps.

When the nodes have booted up, you should see a message like the following 
in the console: 

     Node started up and registered in 5.007 sec