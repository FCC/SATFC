import ch.qos.logback.classic.Level

def ROOT_LOG_LEVEL_PROPERTY = System.getProperty("SATFC.root.log.level")
def ROOT_LOG_LEVEL = ROOT_LOG_LEVEL_PROPERTY == null ? Level.INFO : Level.valueOf(ROOT_LOG_LEVEL_PROPERTY)
println "Setting root log level to $ROOT_LOG_LEVEL"

def LOG_FILE_NAME = System.getProperty("SATFC.log.filename")

def LOG_FORMAT = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{0} - %msg%n"

if (LOG_FILE_NAME != null) {
    appender("FILE", FileAppender) {
        file = LOG_FILE_NAME
        encoder(PatternLayoutEncoder) {
            pattern = LOG_FORMAT
        }
    }
}

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = LOG_FORMAT
    }
}

root(ROOT_LOG_LEVEL, ["CONSOLE", "FILE"])