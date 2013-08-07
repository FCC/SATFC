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

$(TARGET):  $(FLAGS) $(LIBS) $(DEPS) $(OBJECTS)
	$(CXX) $(WARNFLAGS) $(CXXFLAGS) $(LDFLAGS) $(OBJECTS) $(LIBS) $(LDLIBS) -o $(TARGET)
ifneq ($(POST_BUILD),)
	$(POST_BUILD) $(TARGET)
endif

ifneq ($(MAKECMDGOALS),clean)
-include $(DEPS)
endif

.PHONY: clean install $(SUBDIRS)

# NOTE: empty command for LIBS - must be made by subdir makes
$(LIBS): $(SUBDIRS) ;

# Rule for making subdir targets
$(SUBDIRS):
	@$(MAKE) -C $@ $(findstring clean, $(MAKECMDGOALS))


$(FLAGS):
	$(error No configuration found - please run $(PROJECT_ROOT)/configure first)

ifeq ($(findstring Windows,$(OS)),)
install: $(TARGET)
	 $(INSTALL) $(TARGET) $(INSTALL_DIR)

clean: $(SUBDIRS)
	@$(RM) $(TARGET)
	@$(RM) $(OBJECTS)
	@$(RM) $(DEPS)
else
install: $(TARGET)
	 copy $(subst /,\,$(TARGET)) $(INSTALL_DIR)
RM := del /F /Q
clean: $(SUBDIRS)
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

