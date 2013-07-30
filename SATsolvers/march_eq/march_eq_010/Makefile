CFLAGS  =  	-s -O3 -fomit-frame-pointer -march=pentiumpro -malign-functions=4 -funroll-loops -fexpensive-optimizations -malign-double -fschedule-insns2 -Wall #CC 	=  	gcc-3.0  

#CFLAGS = 	-Wall -O3 -ggdb -pg
CC	= 	gcc

.c.o:
		$(CC) -c $(CFLAGS) $(SATTEST) -o $@ $<   

vicii: vicii.o \
		debug.o \
		parser.o \
		solver.o \
		flads.o \
		lookahead.o \
		equivalence.o \
		memory.o \
		mar.o \
		progressBar.o
		$(CC) $(CFLAGS) $(SATTEST) *.o -lm -o solve --static
		
vicii.o: vicii.c \
		vicii.h \
		common.h \
		debug.h \
		parser.h \
		solver.h \
		mar.h \
		progressBar.h

debug.o: debug.c \
		debug.h

parser.o: parser.c \
		parser.h \
		common.h \
		equivalence.h \
		debug.h

solver.o: solver.c \
		solver.h \
		common.h \
		lookahead.h \
		debug.h \
		mar.h \
		progressBar.h

mar.o: mar.c \
		mar.h \
		common.h \
		debug.h

flads.o: flads.c \
		flads.h

lookahead.o: lookahead.c \
		lookahead.h \
		common.h \
		flads.h \
		debug.h \
		solver.h

equivalence.o: equivalence.c \
		equivalence.h \
		memory.h

memory.o: memory.c \
		memory.h

progressBar.o: progressBar.c \
		progressBar.h \
		common.h \
		debug.h

clean:
	rm *.o -f


