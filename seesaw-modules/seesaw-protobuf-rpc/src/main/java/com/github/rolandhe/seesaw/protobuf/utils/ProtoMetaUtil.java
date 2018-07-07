package com.github.rolandhe.seesaw.protobuf.utils;

import com.google.protobuf.BlockingService;
import com.google.protobuf.Service;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rolandhe on 2018/6/30.
 */
public class ProtoMetaUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtoMetaUtil.class);

    public static final String PROTO_SERVICE_INTERFACENAME = "$BlockingInterface";

    private ProtoMetaUtil(){}

    public static boolean checkServiceInstance(Class<?> serviceClass) {

       return obtainProtoServiceClassFromInstance(serviceClass).size() > 0;
    }

    public static List<BlockingService> obtainServiceClassFromInstance(Class<?> serviceClass,Object serviceInstance) {
        List<BlockingService> serviceList = new ArrayList<>();

        List<Class<?>> protoServiceList = obtainProtoServiceClassFromInstance(serviceClass);

        for(Class<?> protoServiceClass : protoServiceList) {
            try {
                BlockingService service =
                        (BlockingService) MethodUtils.invokeStaticMethod(protoServiceClass,
                                "newReflectiveBlockingService", serviceInstance);
                serviceList.add(service);

            } catch (NoSuchMethodException|IllegalAccessException e) {
                LOGGER.info(null,e);
            } catch (InvocationTargetException e) {
                LOGGER.error(null, e.getTargetException());
                throw new RuntimeException(e.getTargetException());
            }
        }
        return serviceList;
    }

    public static List<Class<?>> obtainProtoServiceClassFromInstance(Class<?> serviceClass) {
        List<Class<?>> list = new ArrayList<>();
        Class<?>[] intefaceArray = serviceClass.getInterfaces();
        for(Class<?> iface : intefaceArray) {
            if(!iface.getName().endsWith(PROTO_SERVICE_INTERFACENAME)) {
                continue;
            }
            Class<?> clazz = obtainServiceClass(iface.getName());
            if(clazz != null) {
                list.add(clazz);
            }
        }
        return list;
    }

    public static Class<?> obtainServiceClass(String interfaceName) {
        String className = interfaceName.substring(0,interfaceName.length() - PROTO_SERVICE_INTERFACENAME.length());
        try {
            Class<?> clazz = Class.forName(className);
            if(!Service.class.isAssignableFrom(clazz)) {
                LOGGER.info("{} is not proto service.", className);
                return null;
            }

            return clazz;
        } catch (ClassNotFoundException e) {
            LOGGER.error(null, e);
        }
        return null;
    }
}
