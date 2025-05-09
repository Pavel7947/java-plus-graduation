package ru.practicum.ewm.client.request;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.api.RequestAdminResource;

@FeignClient(name = "request-service", fallback = RequestServiceClientFallback.class)
public interface RequestServiceClient extends RequestAdminResource {
}
