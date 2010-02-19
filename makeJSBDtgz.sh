## uncomment the build part in pom.xml

## before creating a tar-ball for distribution to third party, you might want to add some
## trained models (if so, add then into directory models)

# make binaries
mvn clean
mvn dependency:copy-dependencies
mvn package -Dmaven.test.skip=true
mkdir lib
rm lib/*
cp target/JSBD-2.4.jar lib/.
cp target/dependency/*.jar lib/.
rm lib/uima*.jar

# make documentation
cd doc
pdflatex JSBD.tex
pdflatex JSBD.tex
bibtex JSBD
pdflatex JSBD.tex
cp JSBD.pdf ../JSBD-2.4.pdf
cd ..


# do the packaging
tar -czvf JSBD-2.4.tgz runJSBDpackaged.sh src lib testdata models \
README LICENSE COPYRIGHT JSBD-2.4.pdf \
--exclude=".svn" --exclude "*jules*java" --exclude "*src/test*" --exclude "julie*types.xml"

rm JSBD-2.4.pdf

