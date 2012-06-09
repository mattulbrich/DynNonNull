rm -rf classes
mkdir classes
cd classes
jar xf ../bcel.jar
cd ..

javac -cp classes -d bin $(find src -name "*.java") 

jar cmf MANIFEST.MF nonnull.jar -C classes .
rm -rf classes
