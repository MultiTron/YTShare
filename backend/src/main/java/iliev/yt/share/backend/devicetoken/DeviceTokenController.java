package iliev.yt.share.backend.devicetoken;

import iliev.yt.share.backend.devicetoken.dto.DeviceTokenInputDto;
import iliev.yt.share.backend.devicetoken.dto.DeviceTokenOutputDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {
    private final DeviceTokenService deviceTokenService;

    @PostMapping
    public DeviceTokenOutputDto registerToken(@RequestBody final DeviceTokenInputDto inputDto) {
        return deviceTokenService.registerToken(inputDto);
    }

    @DeleteMapping
    public void removeToken() {
        deviceTokenService.removeTokensForCurrentUser();
    }
}
