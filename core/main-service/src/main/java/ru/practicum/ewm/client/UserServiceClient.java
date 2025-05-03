package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.api.UserServiceAdminResource;

@FeignClient(name = "user-service")
public interface UserServiceClient extends UserServiceAdminResource {

}
