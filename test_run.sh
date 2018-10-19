mkdir replay
rm -rf replay/*
./gradlew submission
./halite -vvv --width 64 --height 64 --seed 4  --replay-directory replay/ "java -jar build/libs/MyBot.jar" "java -jar build/libs/MyBot.jar"