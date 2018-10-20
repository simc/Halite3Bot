#!/bin/sh
mkdir replay
rm -rf replay/*
./gradlew submission
SEED="$(awk -v min=5 -v max=10000000 'BEGIN{srand(); print int(min+rand()*(max-min+1))}')"
./halite -vvv --width 64 --height 64 --seed $SEED --no-logs --replay-directory replay/ "java -jar bot/build/libs/MyBot.jar" "java -jar TestBot.jar"