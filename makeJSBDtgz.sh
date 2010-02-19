## uncomment the build part in pom.xml

# make binaries
mvn clean
mvn dependency:copy-dependencies
mvn package -Dmaven.test.skip=true
mkdir lib
rm lib/*
cp target/JSBD-2.4.jar lib/.
cp target/dependency/*.jar lib/.
rm lib/uima*.jar
rm lib/jules-*.jar

# make documentation
cd doc
pdflatex UIMA-JSBD.tex
pdflatex UIMA-JSBD.tex
bibtex UIMA-JSBD
pdflatex UIMA-JSBD.tex
cp UIMA-JSBD.pdf ../UIMA-JSBD-2.4.pdf
cd ..


# do the packaging
tar -czvf JSBD-2.4.tgz src lib testdata models \
JSBD-2.4.jar README LICENSE COPYRIGHT UIMA-JSBD-2.4.pdf \
--exclude=".svn" --exclude "*jules*" --exclude "*src/test*" --exclude "julie*types.xml"

rm UIMA-JSBD.pdf

