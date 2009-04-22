rm -rf bin
mkdir bin
cd bin
jar xf ../bcel.jar
cd ..

javac -cp bin -d bin $(find src -name "*.java") 

jar cf nonnull.jar -C bin .
