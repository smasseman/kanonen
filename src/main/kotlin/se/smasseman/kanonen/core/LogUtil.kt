package se.smasseman.kanonen.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface LogUtil {

}

fun LogUtil.logger() : Logger {
    return LoggerFactory.getLogger(this.javaClass)
}
