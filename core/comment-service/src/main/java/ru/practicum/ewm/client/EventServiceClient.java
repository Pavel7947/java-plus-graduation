package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.api.EventServiceAdminResource;
import ru.practicum.ewm.dto.event.EventFullDto;

import java.util.List;

@FeignClient(name = "main-service")
public interface EventServiceClient extends EventServiceAdminResource {
}
