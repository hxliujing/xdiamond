package io.github.xdiamond.client.spring;

import io.github.xdiamond.client.annotation.AllKeyListener;
import io.github.xdiamond.client.event.ConfigEvent;
import io.github.xdiamond.client.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

@Component
public class XDiamondAllKeyListener {
    protected static final Logger logger = LoggerFactory.getLogger(XDiamondAllKeyListener.class);


    @AllKeyListener
    public void testAllKeyListener(ConfigEvent event) {
        System.err.println("ListenerExampleService, testAllKeyListener, event :" + event);
        logger.info("配置更新:{}", event.toString());
        try {
            if (event == null || StringUtils.isEmpty(event.getKey())
                    || !EventType.UPDATE.equals(event.getEventType())) {
                return;
            }
            XDiamondConfigFactoryBean xDiamondConfigFactoryBean = XDiamondConfigFactoryBean.context.getBean(XDiamondConfigFactoryBean.class);
            Set<Class<?>> classdatas =  xDiamondConfigFactoryBean.getClassData();
            for(Class<?> classdata:classdatas ){
                Object obj = XDiamondConfigFactoryBean.context.getBean(classdata);
                Method[] methods = classdata.getDeclaredMethods();
                if (methods != null) {
                    for (Method method : methods) {
                        Value annotation = method.getAnnotation(Value.class);
                        if (annotation == null)
                            continue;
                        String value = annotation.value();
                        if (!StringUtils.isEmpty(value)) {
                            value = value.substring(value.indexOf("${") + 2, value.lastIndexOf("}")).trim();
                            if (value.equalsIgnoreCase(event.getKey())) {
                                method.invoke(obj, event.getValue());
                                logger.info("配置更新成功:{},{},{}", method.getName(), event.getKey(), event.getValue());
                                return;
                            }
                        }
                    }
                }
                Field[] fields = classdata.getDeclaredFields();
                if (fields != null) {
                    for (Field field : fields) {
                        Value annotation = field.getAnnotation(Value.class);
                        if (annotation == null)
                            continue;
                        String prop = Character.toUpperCase(field.getName().charAt(0)) +
                                field.getName().substring(1);
                        String mname = "set" + prop;
                        String value = annotation.value();
                        if (!StringUtils.isEmpty(value)) {
                            value = value.substring(value.indexOf("${") + 2, value.lastIndexOf("}")).trim();
                            if (value.equalsIgnoreCase(event.getKey())) {
                                if (methods != null) {
                                    for (Method method : methods) {
                                        if(method.getName().equals(mname)){
                                            method.invoke(obj, event.getValue());
                                            logger.info("配置更新成功:{},{},{}", method.getName(), event.getKey(), event.getValue());
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("配置更新失败");
        }
    }
}
