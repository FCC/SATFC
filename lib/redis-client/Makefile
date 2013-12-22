CLASSPATH = `ruby collect_classpath.rb`
JFLAGS = -cp $(CLASSPATH)
JC = javac
JAVA = java
JCFLAGS = -Xlint:unchecked

.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $(JCFLAGS) $*.java

CLASSES = SATFCJobClient.java

default: classes

classes: $(CLASSES:.java=.class)

run: classes
	$(JAVA) $(JFLAGS) SATFCJobClient $(ARGS)

clean:
	$(RM) *.class
