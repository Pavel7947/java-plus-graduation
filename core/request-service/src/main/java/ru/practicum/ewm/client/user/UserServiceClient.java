package ru.practicum.ewm.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.api.UserServiceAdminResource;

@FeignClient(name = "user-service", fallbackFactory = UserServiceClientFallbackFactory.class)
public interface UserServiceClient extends UserServiceAdminResource {

}
