# [RecMeApp](http://recmeapp.com)

RecMeApp recommends venues by analyzing your Foursquare checkins.

## How it works

1. Get check-in's from 4sq
2. Use Factual's crosswalk api to get from 4sq venue id -> factual venue id
3. Use factual venue id to look up similar venues using Locu
4. Do some filtering and processing on the results
5. Create a 4sq list with the results

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
