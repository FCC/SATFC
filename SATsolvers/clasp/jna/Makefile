-include CLASP_CONFIG

-include PROJECT_ROOT

TARGET       := libjnaclasp.so
FLAGS        := $(PROJECT_ROOT)/build/jna/FLAGS
SOURCE_DIR   := "./"
INCLUDE_DIR  := "./"
OUT_DIR      := "./"
INCLUDES     := -I$(PROJECT_ROOT)/libclasp -I$(PROJECT_ROOT)/libprogram_opts -I$(PROJECT_ROOT)/app -I$(PROJECT_ROOT)
SUBDIRS      := 
LIBS         := libclasp.so libprogram_opts.so
INSTALL_DIR  := "./"
OBJECTS	     := $(PROJECT_ROOT)/build/jna/app/clasp_options.o


-include $(FLAGS)

$(TARGET): jna_clasp.o
	$(CXX) $(WARNFLAGS) $(CXXFLAGS) $(LDFLAGS) $(OBJECTS) jna_clasp.o $(LIBS) $(LDLIBS) --shared -o $(TARGET)

jna_clasp.o: jna_clasp.cpp
	$(CXX) $(INCLUDES) $(WARNFLAGS) $(CXXFLAGS) -c $< -o $@

clean:
	rm *.so *.o

