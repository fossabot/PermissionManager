package it.orangee.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import it.orangee.annotation.AskPermission;
import it.orangee.element.AnnotateMethod;
import it.orangee.element.AskPermissionElement;

public class Processor extends AbstractProcessor {

    private boolean processed;


    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (processed) {
            return true;
        } else {
            processed = true;
        }

        List<AskPermissionElement> annotatedElements = new ArrayList<>();

        for (final Element element : roundEnvironment.getElementsAnnotatedWith(AskPermission.class)) {


            if (element.getKind() == ElementKind.CLASS) {
                final TypeElement typeElement = (TypeElement) element;
                if (!isSubtypeOfActivity(typeElement.asType())) {
                    // error(element, "Only activities can be annotated with @%s", Shortcut.class.getSimpleName());
                    return true;
                }
//nessuna classe dovrebbe essere annotata
                // annotatedElements.add(new ShortcutAnnotatedClass(typeElement));
            } else if (element.getKind() == ElementKind.METHOD) {
                final ExecutableElement executableElement = (ExecutableElement) element;

                final Element enclosingElement = executableElement.getEnclosingElement();

                if (!isSubtypeOfActivity(enclosingElement.asType())) {
                    //     error(element, "Methods annotated with @%s must be part of activities", Shortcut.class.getSimpleName());
                    return true;
                }

                if (!executableElement.getModifiers().contains(Modifier.PUBLIC)) {
                    error(element, "Methods annotated with @%s must be public", AskPermission.class.getSimpleName());
                    return true;
                }

                if (executableElement.getParameters().size() > 0) {
                    error(element, "Methods annotated with @%s can't have parameters", AskPermission.class.getSimpleName());
                    return true;
                }

                annotatedElements.add(new AnnotateMethod(executableElement));
            } else {
                // error(element, "Only classes and methods can be annotated with @", Shortcut.class.getSimpleName());
                return true;
            }

        }
            new CodeGenerator(processingEnv.getFiler(), annotatedElements).generate();
        return false;
    }

    private boolean isSubtypeOfActivity(TypeMirror typeMirror) {
        if ("android.app.Activity".equals(typeMirror.toString())) {
            return true;
        }

        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }

        DeclaredType declaredType = (DeclaredType) typeMirror;
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }

        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        return isSubtypeOfActivity(superType);
    }


    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
