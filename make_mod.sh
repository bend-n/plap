echo Making jar
./gradlew jar
echo Copying
cp ./build/libs/* ~/Documents/mindustry/game/server/plague1-server/config/mods

