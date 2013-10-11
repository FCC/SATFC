-include CLASP_CONFIG

-include PROJECT_ROOT

TARGET       := libjnaclasp.so
FLAGS        := $(PROJECT_ROOT)/build/jna/FLAGS
SOURCE_DIR   := "./"
INCLUDE_DIR  := "./"
OUT_DIR      := "./"
INCLUDES     := -I$(PROJECT_ROOT)/libclasp -I$(PROJECT_ROOT)/libprogram_opts -I$(PROJECT_ROOT)/app -I$(PROJECT_ROOT)
SUBDIRS      := 
#LIBS         := libclasp.so libprogram_opts.so
LIBS         := libclasp.a libprogram_opts.a
INSTALL_DIR  := "./"
OBJECTS	     := $(filter-out $(PROJECT_ROOT)/build/jna/app/main.o, $(wildcard $(PROJECT_ROOT)/build/jna/app/*.o))

-include $(FLAGS)

$(TARGET): jna_clasp.o jna_incclasp.o
	$(CXX) $(WARNFLAGS) $(CXXFLAGS) $(LDFLAGS) $(OBJECTS) jna_clasp.o jna_incclasp.o $(LIBS) $(LDLIBS) --shared -o $(TARGET)

jna_clasp.o: jna_clasp.cpp
	$(CXX) $(INCLUDES) $(WARNFLAGS) $(CXXFLAGS) -c $< -o $@

jna_incclasp.o: jna_incclasp.cpp
	$(CXX) $(INCLUDES) $(WARNFLAGS) $(CXXFLAGS) -c $< -o $@

clean:
	rm *.so *.o *.a

clean1:
	rm *.o *.a
