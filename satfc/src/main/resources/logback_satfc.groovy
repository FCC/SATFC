/*
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
import ch.qos.logback.classic.Level

def ROOT_LOG_LEVEL_PROPERTY = System.getProperty("SATFC.root.log.level")
def ROOT_LOG_LEVEL = ROOT_LOG_LEVEL_PROPERTY == null ? Level.INFO : Level.valueOf(ROOT_LOG_LEVEL_PROPERTY)
println "Setting root log level to $ROOT_LOG_LEVEL"

def LOG_FILE_NAME = System.getProperty("SATFC.log.filename")

def LOG_FORMAT = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{0} - %msg%n"

if (LOG_FILE_NAME != null) {
    appender("FILE", FileAppender) {
        file = LOG_FILE_NAME
        append = false
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