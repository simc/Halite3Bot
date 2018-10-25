mkdir replay
rm -rf replay/*
./gradlew buildDebug
./halite -vvv --width 64 --height 64 --no-timeout --replay-directory replay/ "java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006,quiet=y -jar bot/build/libs/MyBot.jar" "java -jar TestBot.jar" &
sleep 4