CLASSPATH = `ruby collect_classpath.rb`
JFLAGS = -Xmx1024m -classpath $(CLASSPATH)
JC = javac
JAVA = java
JDB = jdb
JCFLAGS = -J-Xmx1024m -g -Xlint:unchecked -classpath $(CLASSPATH)

.SUFFIXES: .java .class
.java.class:
	$(JC) $(JCFLAGS) $*.java

CLASSES = SATFCJobClient.java

default: classes

classes: $(CLASSES:.java=.class)

run: classes
	$(JAVA) $(JFLAGS) SATFCJobClient $(ARGS)

run_db: classes
	$(JDB) $(JFLAGS) SATFCJobClient $(ARGS)

clean:
	$(RM) *.class
	# Removing all class files is in order.
	# It does confuse git because they are expected to be placed in jars.
	# The easist solution is to just use Eclipse.
	find ../../ -name *.class -exec rm -f '{}' \;


try_job_client: job_caster.rb q_and_a_handling.rb redis_retry.rb tokens.rb json_parsing.rb

job_caster.rb: ../../../app/models/job_caster.rb
	cp $< .

q_and_a_handling.rb: ../../../app/models/q_and_a_handling.rb
	cp $< .
	
redis_retry.rb: ../../../app/models/redis_retry.rb
	cp $< .

tokens.rb: ../../../tokens.rb
	cp $< .

json_parsing.rb: ../../../app/models/json_parsing.rb
	cp $< .
