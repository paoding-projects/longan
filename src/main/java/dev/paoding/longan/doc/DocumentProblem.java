package dev.paoding.longan.doc;

import dev.paoding.longan.annotation.Param;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.attribute.LineNumberTableAttribute;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public class DocumentProblem {
    private String cause;
    private String type;
    private Param param;
    private Method method;
    private Field field;

    public DocumentProblem(String cause, Method method) {
        this.cause = cause;
        this.method = method;
        this.type = "Request";
    }

    public DocumentProblem(String cause, String type, Method method) {
        this.cause = cause;
        this.method = method;
        this.type = type;
    }

    public DocumentProblem(String cause, Param param, Method method) {
        this.cause = cause;
        this.param = param;
        this.method = method;
        this.type = "Request";
    }

    public DocumentProblem(String cause, Field field) {
        this.cause = cause;
        this.field = field;
        this.type = "Column";
    }

    @Override
    public String toString() {
        if (type.endsWith("Column")) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n\t--------------------------------------------------------------------");
            sb.append("\n\ttype: " + type);
            sb.append("\n\tcause: " + cause);
            if (param != null) {
                sb.append("\n\tparam: " + param.name());
            }
            sb.append("\n\tfield: " + field.getDeclaringClass().getName() + "." + field.getName() + "(" + field.getDeclaringClass().getSimpleName() + ".java:" + 1 + ")");
            sb.append("\n\t--------------------------------------------------------------------");
            return sb.toString();
        } else {
            int lineNumber = getLineNumber(method);
            StringBuilder sb = new StringBuilder();
            sb.append("\n\t--------------------------------------------------------------------");
            sb.append("\n\ttype: " + type);
            sb.append("\n\tcause: " + cause);
            if (param != null) {
                sb.append("\n\tparam: " + param.name());
            }
            sb.append("\n\tmethod: " + method.getDeclaringClass().getName() + "." + method.getName() + "(" + method.getDeclaringClass().getSimpleName() + ".java:" + lineNumber + ")");
            sb.append("\n\t--------------------------------------------------------------------");
            return sb.toString();
        }
    }

    private int getLineNumber(Method method) {
        Class<?> clazz = method.getDeclaringClass();
        String path = clazz.getName().replace('.', '/') + ".class";
        byte[] bytes;
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(path)) {
            assert is != null;
            bytes = is.readAllBytes();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClassModel classModel = ClassFile.of().parse(bytes);
        for (MethodModel methodModel : classModel.methods()) {
            if (matchesMethod(method, methodModel)) {
                return getLineNumber(methodModel);
            }
        }
        return 0;
    }

    private boolean matchesMethod(Method method, MethodModel methodModel) {
        if (methodModel.methodName().stringValue().equals(method.getName()) && methodModel.methodTypeSymbol().parameterCount() == method.getParameterCount()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            ClassDesc[] parameterArray = methodModel.methodTypeSymbol().parameterArray();
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!parameterTypes[i].getSimpleName().equals(parameterArray[i].displayName())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private int getLineNumber(MethodModel method) {
        Optional<CodeAttribute> CodeAttributeOptional = method.findAttribute(Attributes.code());
        if (CodeAttributeOptional.isPresent()) {
            CodeAttribute codeAttribute = CodeAttributeOptional.get();
            Optional<LineNumberTableAttribute> LineNumberTableAttributeOptional = codeAttribute.findAttribute(Attributes.lineNumberTable());
            if (LineNumberTableAttributeOptional.isPresent()) {
                LineNumberTableAttribute lineNumberTableAttribute = LineNumberTableAttributeOptional.get();
                return lineNumberTableAttribute.lineNumbers().getFirst().lineNumber();
            }
        }
        return 0;
    }
}
