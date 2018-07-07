package com.github.rolandhe.seesaw.protobuf.server;

import com.github.rolandhe.seesaw.protobuf.utils.ProtoMetaUtil;
import com.google.protobuf.BlockingService;
import com.google.protobuf.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rolandhe on 2018/6/30.
 */
public class ServiceInstanceRegister {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInstanceRegister.class);

    private static final Map<String, ServiceMethod> METHOD_CACHE = new ConcurrentHashMap<>();

    public static void registerServiceInstance(Class<?> clazz, Object serviceInstance) {
        if (!ProtoMetaUtil.checkServiceInstance(clazz)) {
            LOGGER.error("{} is not proto service.", serviceInstance.getClass().getName());
            throw new RuntimeException(serviceInstance.getClass().getName() + " is not proto service.");
        }

        List<BlockingService> list = ProtoMetaUtil.obtainServiceClassFromInstance(clazz, serviceInstance);


        for (BlockingService service : list) {
            List<Descriptors.MethodDescriptor> ds = service.getDescriptorForType().getMethods();
            for (Descriptors.MethodDescriptor md : ds) {
                METHOD_CACHE.put(md.getFullName(), new ServiceMethod(service, md));
            }
        }
    }

    public static ProtobufMethod getMethodByFullName(String methodName) {
        return METHOD_CACHE.get(methodName);
    }
}
