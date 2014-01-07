CLASSPATH = `ruby collect_classpath.rb`
JFLAGS = -classpath $(CLASSPATH)
JC = javac
JAVA = java
JDB = jdb
JCFLAGS = -g -Xlint:unchecked

.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $(JCFLAGS) $*.java

CLASSES = SATFCJobClient.java

default: classes

classes: $(CLASSES:.java=.class)

run: classes
	$(JAVA) $(JFLAGS) SATFCJobClient $(ARGS)

run_db: classes
	$(JDB) $(JFLAGS) SATFCJobClient $(ARGS)

clean:
	$(RM) *.class
