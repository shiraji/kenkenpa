package com.github.shiraji.kenkenpa.compiler;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.ASSERT;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

public class KenKenPaTestHelper {
    public static void compareSourceCodesWithoutError(JavaFileObject source, JavaFileObject expectedSource) {
        ASSERT.about(javaSource())
                .that(source)
                .processedWith(new KenKenPaProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedSource);
    }

    public static void compileShouldFail(JavaFileObject source) {
        ASSERT.about(javaSource())
                .that(source)
                .processedWith(new KenKenPaProcessor())
                .failsToCompile();
    }
}
