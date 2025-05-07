package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.api.RequestAdminResource;

@FeignClient(name = "request-service")
public interface RequestServiceClient extends RequestAdminResource {
}
