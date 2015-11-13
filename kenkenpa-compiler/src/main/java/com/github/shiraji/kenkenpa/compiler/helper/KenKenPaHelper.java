package com.github.shiraji.kenkenpa.compiler.helper;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;

import static javax.tools.Diagnostic.Kind.ERROR;

public class KenKenPaHelper {

    public static void logParsingError(ProcessingEnvironment processingEnv, Element element,
                                 Class<? extends Annotation> annotation, Exception e) {
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        error(processingEnv, element, "Unable to parse @%s.\n\n%s",
                annotation.getSimpleName(), stackTrace);
    }

    private static void error(ProcessingEnvironment processingEnv,Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(ERROR, message, element);
    }

}
