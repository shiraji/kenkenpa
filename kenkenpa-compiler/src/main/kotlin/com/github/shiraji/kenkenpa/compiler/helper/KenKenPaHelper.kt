package com.github.shiraji.kenkenpa.compiler.helper

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import java.io.PrintWriter
import java.io.StringWriter

import javax.tools.Diagnostic.Kind.ERROR

class KenKenPaHelper {

    fun logParsingError(processingEnv: ProcessingEnvironment, element: Element, annotation: Class<out Annotation>, e: Exception) {
        val stackTrace = StringWriter()
        e.printStackTrace(PrintWriter(stackTrace))
        printError(processingEnv, element, "Unable to parse @%s.\n\n%s",
                annotation.simpleName, stackTrace)
    }

    private fun printError(processingEnv: ProcessingEnvironment, element: Element, message: String, vararg args: Any) {
        var message = message
        if (args.size > 0) {
            message = message.format(args)
        }
        processingEnv.messager.printMessage(ERROR, message, element)
    }

    fun hasNoParameters(element: Element): Boolean {
        val executableElement = element as ExecutableElement
        val parameters = executableElement.parameters
        return parameters.size == 0
    }
}
