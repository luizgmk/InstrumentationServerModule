# InstrumentationServerModule
Trying out some reflection

# But why?
Imagine you have an Android chat application and you want to write automation tests that do not use any mocks, orchestrates multiple instances of the application running in different devices and you don't don't want to rely on UI automations. Imagine you also don't want to write (mostly) any Android code and want to keep your app clean. 

By exposing internal dependencies as REST API's it's possible to orchestrate multiple devices and do some level of automation testing with any scripting tool, even shell scripts (using curl).

# DISCLAIMER
There is a terrible leak. Don't use for Flows until fixing it. Find the TODO.

# How it works

Add the dependency, initialize exposing some classes and that's it.
Below contents are an excerpt from App.kt file fo sample app.
```
// Instrumentation Server enables REST calls to the exposed classes declared below
        if (BuildConfig.DEBUG) MainScope().launch {
            withContext(Dispatchers.IO) {
                val server = InstrumentationServer()

                // Serve singleton classes injected by DI
                val db: DogBreedsDB by inject()
                val repo: DogBreedsRepository by inject()
                server.addSingleton(db)
                server.addSingleton(repo)

                // Serve also DogBreed class for individual instantiation per call
                server.addClass(DogBreed::class)

                // launch the server on port 8080
                server.start(port = 8080)
            }
        }
```

# Using the instrumented classes

```
# forward the port
adb forward tcp:8080 tcp:8080

# Check exposed classes and instances
curl -s http://localhost:8080/info | jq
curl -s http://localhost:8080/DogBreedsRepository/info | jq
curl -s http://localhost:8080/DogBreedsRepository/add/info | jq

# Read out dog breeds from the repository
curl -sX POST -H "Content-Type: application/json" http://localhost:8080/DogBreedsRepository/breeds | jq

# Push a new breed via repository (dynamically create DogBreed instance to pass as parameter)
curl -sX POST -H "Content-Type: application/json" -d '{"com.example.jc.model.DogBreed": {"name":"New Breed"}}' http://localhost:8080/DogBreedsRepository/add | jq

# Check again to see update (also on sample app screen)
curl -sX POST -H "Content-Type: application/json" http://localhost:8080/DogBreedsRepository/breeds | jq
```

# Checking open ports by the app

Release builds won't contain the instrumentation server. The steps below allow to confirm it.

```
# simply make a call
curl http://localhost:8080/info
# get pid
top -s 6 | grep example
# check open ports (replace pid)
lsof | grep 11107 | egrep 'TCP|UDP' | grep -v CLOSE_WAIT
```
