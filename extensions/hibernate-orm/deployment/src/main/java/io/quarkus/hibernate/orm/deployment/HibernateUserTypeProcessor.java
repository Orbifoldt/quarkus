package io.quarkus.hibernate.orm.deployment;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public final class HibernateUserTypeProcessor {
    private static final String TYPE_VALUE = "type";
    private static final String TYPE_CLASS_VALUE = "typeClass";

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndexBuildItem) {
        IndexView index = combinedIndexBuildItem.getIndex();

        final Set<String> userTypes = new HashSet<>();

        Collection<AnnotationInstance> typeAnnotationInstances = index.getAnnotations(ClassNames.TYPE);
        Collection<AnnotationInstance> typeDefinitionAnnotationInstances = index.getAnnotations(ClassNames.TYPE_DEFINITION);
        Collection<AnnotationInstance> typeDefinitionsAnnotationInstances = index.getAnnotations(ClassNames.TYPE_DEFINITIONS);

        userTypes.addAll(getUserTypes(typeDefinitionAnnotationInstances));

        for (AnnotationInstance typeDefinitionAnnotationInstance : typeDefinitionsAnnotationInstances) {
            final AnnotationValue typeDefinitionsAnnotationValue = typeDefinitionAnnotationInstance.value();

            if (typeDefinitionsAnnotationValue == null) {
                continue;
            }

            userTypes.addAll(getUserTypes(Arrays.asList(typeDefinitionsAnnotationValue.asNestedArray())));
        }

        for (AnnotationInstance typeAnnotationInstance : typeAnnotationInstances) {
            final AnnotationValue typeValue = typeAnnotationInstance.value(TYPE_VALUE);
            if (typeValue == null) {
                continue;
            }

            final String type = typeValue.asString();
            final DotName className = DotName.createSimple(type);
            if (index.getClassByName(className) == null) {
                continue; // Either already registered through TypeDef annotation scanning or not present in the index
            }
            userTypes.add(type);
        }

        if (!userTypes.isEmpty()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, userTypes.toArray(new String[] {})));
        }
    }

    private Set<String> getUserTypes(Collection<AnnotationInstance> typeDefinitionAnnotationInstances) {
        final Set<String> userTypes = new HashSet<>();

        for (AnnotationInstance typeDefAnnotationInstance : typeDefinitionAnnotationInstances) {
            final AnnotationValue typeClassValue = typeDefAnnotationInstance.value(TYPE_CLASS_VALUE);
            if (typeClassValue == null) {
                continue;
            }

            final String typeClass = typeClassValue.asClass().name().toString();
            userTypes.add(typeClass);
        }

        return userTypes;
    }
}
