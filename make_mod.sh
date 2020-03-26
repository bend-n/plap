echo Making jar
./gradlew jar
echo Copying
cp ./build/libs/* ~/Documents/mindustry/game/server/hub-server/config/mods

