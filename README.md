# [RecMeApp](http://recmeapp.com)

RecMeApp recommends venues by analyzing your Foursquare checkins. This was built for TechCrunch
Disrupt hackathon 2012. [Presentation video](http://video.aoljobs.com/recme-demo-517474034)

## How it works

1. Get check-in's from 4sq
2. Use Factual's crosswalk api to get from 4sq venue id -> factual venue id
3. Use factual venue id to look up similar venues using Locu
4. Do some filtering and processing on the results
5. Create a 4sq list with the results

This app showcases the async capabilities of the play framework using Promises and Akka.

## Developing

RecMeApp is build on the play framework. You'll need play 2.0.2 or later. 

Add in the keys below in conf/application.conf

```
4sq.publickey=
4sq.secretkey=

factual.publickey=
factual.secretkey=

locu.apikey=
```

Then run `play run` to start the server

## Deploying

A Profile is included for deploying to Heroku. Simply set your heroku remote correctly then do 

```
$ heroku git:remote -a <your-heroku-app>
$ git push heroku master
```

## Caveats

The factual api is flakey and doesn't always return a result. You may want ot wrap falls to factual
with some retry and error handling logic. 

## Things to improve

There are many areas to improve the code since it was written quickly for a hackathon.

* Use `getQueryParams` of
  [WSRequestHolder](http://www.playframework.org/documentation/api/2.0.2/scala/index.html#play.api.libs.ws.WS$$WSRequestHolder) instead of hand rolling it and using URLEncoder
* Parse results from the APIs to case classes defined in model directory
* Batch falls to Factual and Locu
