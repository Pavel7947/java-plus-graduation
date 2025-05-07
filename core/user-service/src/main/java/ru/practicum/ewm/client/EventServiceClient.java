package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.api.EventServiceAdminResource;

@FeignClient(name = "main-service")
public interface EventServiceClient extends EventServiceAdminResource {
}
