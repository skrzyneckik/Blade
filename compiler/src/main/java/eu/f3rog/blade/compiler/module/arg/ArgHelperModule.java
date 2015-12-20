package eu.f3rog.blade.compiler.module.arg;

import android.app.Fragment;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import blade.Arg;
import eu.f3rog.blade.compiler.ErrorMsg;
import eu.f3rog.blade.compiler.builder.BaseClassBuilder;
import eu.f3rog.blade.compiler.builder.ClassManager;
import eu.f3rog.blade.compiler.builder.MiddleManBuilder;
import eu.f3rog.blade.compiler.builder.helper.BaseHelperModule;
import eu.f3rog.blade.compiler.builder.helper.HelperClassBuilder;
import eu.f3rog.blade.compiler.module.BundleUtil;
import eu.f3rog.blade.compiler.name.EClass;
import eu.f3rog.blade.compiler.util.ProcessorError;
import eu.f3rog.blade.compiler.util.ProcessorUtils;
import eu.f3rog.blade.core.BundleWrapper;

import static eu.f3rog.blade.compiler.util.ProcessorUtils.cannotHaveAnnotation;

/**
 * Class {@link ArgHelperModule}
 *
 * @author FrantisekGazo
 * @version 2015-12-15
 */
public class ArgHelperModule extends BaseHelperModule {

    private static final String METHOD_NAME_INJECT = "inject";

    private static final String ARG_ID_FORMAT = "<Arg-%s>";

    public static String getArgId(VariableElement extra) {
        return String.format(ARG_ID_FORMAT, extra.getSimpleName().toString());
    }

    private final List<VariableElement> mArgs = new ArrayList<>();

    @Override
    public void checkClass(TypeElement e) throws ProcessorError {
        if (!ProcessorUtils.isSubClassOf(e, EClass.SupportFragment.getName(), ClassName.get(Fragment.class))) {
            throw new ProcessorError(e, ErrorMsg.Invalid_class_with_Arg);
        }
    }

    @Override
    public void add(VariableElement e) throws ProcessorError {
        if (cannotHaveAnnotation(e)) {
            throw new ProcessorError(e, ErrorMsg.Invalid_field_with_annotation, Arg.class.getSimpleName());
        }

        mArgs.add(e);
    }

    @Override
    public void implement(HelperClassBuilder builder) throws ProcessorError {
        addInjectMethod(builder);
        addMethodToFragmentFactory(builder);
        addCall(builder);
    }

    private void addInjectMethod(BaseClassBuilder builder) {
        String target = "target";
        MethodSpec.Builder method = MethodSpec.methodBuilder(METHOD_NAME_INJECT)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(builder.getArgClassName(), target);

        method.beginControlFlow("if ($N.getArguments() == null)", target)
                .addStatement("return")
                .endControlFlow();

        String args = "args";
        method.addStatement("$T $N = $T.from($N.getArguments())", BundleWrapper.class, args, BundleWrapper.class, target);

        BundleUtil.getFromBundle(method, target, mArgs, ARG_ID_FORMAT, args);

        builder.getBuilder().addMethod(method.build());
    }

    private void addMethodToFragmentFactory(HelperClassBuilder builder) throws ProcessorError {
        ClassManager.getInstance()
                .getSpecialClass(FragmentFactoryBuilder.class)
                .addMethodFor(builder.getTypeElement());
    }

    private void addCall(BaseClassBuilder builder) {
        ClassManager.getInstance()
                .getSpecialClass(MiddleManBuilder.class)
                .addCall(builder, METHOD_NAME_INJECT);
    }

    public List<VariableElement> getArgs() {
        return mArgs;
    }
}