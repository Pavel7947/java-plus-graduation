package ru.practicum.ewm.client.event;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.api.EventServiceAdminResource;

@FeignClient(name = "event-service", fallbackFactory = EventServiceClientFallbackFactory.class)
public interface EventServiceClient extends EventServiceAdminResource {
}
