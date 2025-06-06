package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.api.EventServiceAdminResource;

@FeignClient(name = "event-service", fallbackFactory = EventServiceClientFallbackFactory.class)
public interface EventServiceClient extends EventServiceAdminResource {
}
