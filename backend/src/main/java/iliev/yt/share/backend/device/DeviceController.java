package iliev.yt.share.backend.device;

import iliev.yt.share.backend.device.dto.DeviceInputDto;
import iliev.yt.share.backend.device.dto.DeviceOutputDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    @GetMapping
    public Page<DeviceOutputDto> getAllDevices(final Pageable pageable) {
        return deviceService.getAllDevices(pageable);
    }

    @GetMapping("/all")
    public List<DeviceOutputDto> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @GetMapping("/{id}")
    public DeviceOutputDto getDeviceById(@PathVariable final UUID id) {
        return deviceService.getDeviceById(id);
    }

    @GetMapping("/user-preferences/{userPreferencesId}")
    public List<DeviceOutputDto> getDevicesByUserPreferencesId(@PathVariable final UUID userPreferencesId) {
        return deviceService.getDevicesByUserPreferencesId(userPreferencesId);
    }

    @PostMapping
    public DeviceOutputDto createDevice(@RequestBody final DeviceInputDto inputDto) {
        return deviceService.createDevice(inputDto);
    }

    @PutMapping("/{id}")
    public DeviceOutputDto updateDevice(
            @PathVariable final UUID id,
            @RequestBody final DeviceInputDto inputDto) {
        return deviceService.updateDevice(id, inputDto);
    }

    @DeleteMapping("/{id}")
    public void deleteDevice(@PathVariable final UUID id) {
        deviceService.deleteDevice(id);
    }
}
