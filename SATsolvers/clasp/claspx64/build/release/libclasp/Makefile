SHELL        ?= /bin/sh
CXXFLAGS     ?=
LDFLAGS      ?=
CXX          ?= g++
INSTALL      ?= install
AR           ?= ar
-include .CONFIG
-include $(FLAGS)
SOURCES      := $(patsubst $(SOURCE_DIR)/%.cpp,%.cpp,$(wildcard $(SOURCE_DIR)/*.cpp))
ifeq ($(OUT_DIR),)
DEPS         := $(patsubst %.cpp,%.dep, $(SOURCES))
OBJECTS      := $(patsubst %.cpp,%.o, $(SOURCES))
else
DEPS         := $(patsubst %.cpp,$(OUT_DIR)/%.dep, $(SOURCES))
OBJECTS      := $(patsubst %.cpp,$(OUT_DIR)/%.o, $(SOURCES))
endif

vpath %.cpp $(SOURCE_DIR)
vpath %.h   $(INCLUDE_DIR)

$(TARGET):	$(FLAGS) $(DEPS) $(OBJECTS)
	$(AR)  cvrs $(TARGET) $(OBJECTS)

ifneq ($(MAKECMDGOALS),clean)
-include $(DEPS)
endif

.PHONY:	clean 

ifeq ($(findstring Windows,$(OS)),)
clean:
	@$(RM) $(TARGET)
	@$(RM) $(OBJECTS)
	@$(RM) $(DEPS)
else
RM := del /F /Q
clean:
	@$(RM) $(subst /,\,$(TARGET))  2>nul
	@$(RM) $(subst /,\,$(OBJECTS)) 2>nul
	@$(RM) $(subst /,\,$(DEPS))    2>nul
endif

ifeq ($(OUT_DIR),)
%.dep: %.cpp $(FLAGS)
	$(CXX) $(INCLUDES) $(WARNFLAGS) $(CXXFLAGS) -MM $< > $@

%.o: %.cpp $(FLAGS)
	$(CXX) $(INCLUDES) $(WARNFLAGS) $(CXXFLAGS) -c $< -o $@
else
$(OUT_DIR)/%.dep: %.cpp $(FLAGS)
	$(CXX) $(INCLUDES) $(WARNFLAGS) $(CXXFLAGS) -MT $(OUT_DIR)/$*.o -MM $< > $@

$(OUT_DIR)/%.o: %.cpp $(FLAGS)
	$(CXX) $(INCLUDES) $(WARNFLAGS) $(CXXFLAGS) -c $< -o $@
endif

