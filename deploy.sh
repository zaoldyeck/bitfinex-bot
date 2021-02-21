#!/bin/sh

#sbt assembly
#docker build -t zaoldyeck/bitfinex-bot:latest .
heroku container:push web -a bitfinex-bot-zaoldyeck
heroku container:release web -a bitfinex-bot-zaoldyeck