JEDIS_JAR = jedis-2.1.0.jar
JSON_JAR = gson-2.2.4.jar
JPATH = $(JEDIS_JAR):$(JSON_JAR):.
JFLAGS = -cp $(JPATH)
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
	$(JAVA) $(JFLAGS) SATFCJobClient

clean:
	$(RM) *.class
