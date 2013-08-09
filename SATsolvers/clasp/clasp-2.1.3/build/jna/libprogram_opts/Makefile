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

ifneq ($(TEST_DIR),)
TESTS        := $(patsubst $(TEST_DIR)/%.cpp,%.cpp,$(wildcard $(TEST_DIR)/*.cpp))
ifeq ($(OUT_DIR),)
TEST_DEPS    := $(patsubst %.cpp,%.dep, $(TESTS))
TEST_OBJECTS := $(patsubst %.cpp,%.o, $(TESTS))
else
TEST_DEPS    := $(patsubst %.cpp,$(OUT_DIR)/%.dep, $(TEST_DIR))
TEST_OBJECTS := $(patsubst %.cpp,$(OUT_DIR)/%.o, $(TEST_DIR))
endif

endif

vpath %.cpp $(SOURCE_DIR) $(TEST_DIR)
vpath %.h   $(INCLUDE_DIR)

$(TARGET):	$(FLAGS) $(DEPS) $(OBJECTS)
	$(AR)  cvrs $(TARGET) $(OBJECTS)

ifneq ($(MAKECMDGOALS),clean)
-include $(DEPS)
endif

.PHONY:	clean test

test: $(TEST_TARGET)
	$(TEST_TARGET)

$(TEST_TARGET): $(TARGET) $(TEST_DEPS) $(TEST_OBJECTS) 
	$(CXX) $(WARNFLAGS) $(CXXFLAGS) $(LDFLAGS) $(TEST_OBJECTS) $(LIBS) $(LDLIBS) $(TARGET) -lcppunit -o $(TEST_TARGET)

ifeq ($(findstring Windows,$(OS)),)
clean:
	@$(RM) $(TARGET)
	@$(RM) $(OBJECTS)
	@$(RM) $(DEPS)
	@$(RM) $(TEST_DEPS)
	@$(RM) $(TEST_OBJECTS)
	@$(RM) $(TEST_TARGET)
else
RM := del /F /Q
clean:
	@$(RM) $(subst /,\,$(TARGET))  2>nul
	@$(RM) $(subst /,\,$(OBJECTS)) 2>nul
	@$(RM) $(subst /,\,$(DEPS))    2>nul
	@$(RM) $(subst /,\,$(TEST_OBJECTS)) 2>nul
	@$(RM) $(subst /,\,$(TEST_DEPS))    2>nul
	@$(RM) $(subst /,\,$(TEST_TARGET))    2>nul
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

