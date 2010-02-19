
source setCP.sh

#java -XX:+PrintGCDetails \
#-verbose:gc \
#-XX:+PrintGCTimeStamps \
#-Xmx2000m -cp $CLASSPATH de.julielab.jsbd.SentenceSplitterApplication $*

java -Xmx2000m -cp $CLASSPATH de.julielab.jsbd.SentenceSplitterApplication $*
