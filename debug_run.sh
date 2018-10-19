mkdir replay
rm -rf replay/*
./gradlew buildDebug
./halite -vvv --width 64 --height 64 --no-timeout --replay-directory replay/ "java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005,quiet=y -jar build/libs/MyBot.jar" "java -jar build/libs/MyBot.jar" &
sleep 2