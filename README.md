# drp4camel
Dynamic Route Pulling for Camel

## Introduction
drp4camel provides some components that will help a camel application, to dynamically load new routes,
without any service interruption.

These routes and other resources (such as property files or templates), can be pulled from a remote repository (such as git),
so that the camel application itself can run in a read-only container such as a Docker container.

In order to achieve this several components are provided:
- drp4j: dynamic resource pulling from a remote repository
- RouteLoader: loading of new routes, removing old routes and upgrading existing routes without service interruption
- camel drp endpoint Component: upgrading an endpoint consumer without service interruption

The general algorithm works as follows:
1. pull new routes and other resources from the remote repository
2. load the new routes next to the old routes
3. remove the old routes

These components are described in more detail in the following chapters

## drp4j: dynamic resource pulling from a remote repository
drp4j is a very simple interface that just provides a simple pull() method.
There can be several implementations of this interface:
- NonPuller: does nothing, just use the resources from your file system. Useful for testing
- GitPuller: pull the resources from a git repository, using a "git pull" command
- SvnPuller (planned): pull the resources from a subversion repository using "svn update".
- SftpPuller (planned): copy the resources from a remote fileserver using the sftp protocol
- etc (copying from database, ldap, etc repository

The preferred mechanism would be to use a version control system like git, so that all changes are auditable and the history is always available.


## RouteLoader: loading of new routes, removing old routes and upgrading existing routes without service interruption
A RouteLoader scans all route files in a directory, and possibly subdirectories, and loads these files
while trying to prevent and minimalize disruptions of any service.
- Adding new routes in new files will be no problem, since the services are new.
- Removing routes from deleted files will be no problem, since these services are removed any way.
  The RouteLoader will remember all files loaded and the routes loaded from it.
  When it detects a file doesn't exist anymore it will remove all the routes associated with that file.
- Updating routes from a changed file is a bit more complicated.
  * If a file has not been changed (based on the MD5 hash), it won't be reloaded.
  * If a route does not specify an id, it will generate a unique id for this route, using filename, endpoint and a counter.
    This way the new route can peacefully coexist next to the old route.
  * If a route explicitly specifies an id, it will just reload this route.
    In this case, Camel will automatically stop and remove the old route before loading and starting the new route.
    In this case the service will be disrupted shortly, so it is not encouraged to specify id's for routes that need to be high available.
  * After all routes of the file are loaded, the routes with dynamic names from the old file are, stopped and removed.

The crux of updating routes is that the RouteLoader will assign new route id's to route with no explicit id specified.
This way the old route and new route can peacefully coexist, until the old route is removed.

Because the old route and the updated route need to temporarily coexist, and the will probably listen to the same endpoint,
one extra component is needed, the drp endpoint Camel Component.
This endpoint allows multiple consumers on the same endpoint, and will select the first consumer when a message arrives.
This way when the old and new route are both running, and listening om the same endpoint, the old route will still receive all messages.
As soon as the old route is removed, the new route will receive any new messages.


## camel drp endpoint Component: Dynamic Relay Point
The drp: endpoint is very similar to the standard Camel direct: and direct-vm: endpoints.
In fact the code is copied from the camel DirectVmComponent class and it's supporting classes.

The major difference is that the DrpComponent has a list of consumers for each endpoint
```java
    private final ConcurrentMap<String, List<DrpConsumer>> CONSUMERS = new ConcurrentHashMap<>();
```
while the DirectVmComponent just has one consumer per endpoint.

When the consumer of an endpoint is needed, the DrpComponent looks for the first consumer that is started.
```java
        for (DrpConsumer cons: list) {
            if (cons.isStarted())
                return cons;
```
This way, if a consumer is paused (or removed), the next consumer will take over.

