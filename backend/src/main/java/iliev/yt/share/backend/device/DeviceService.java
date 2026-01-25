package iliev.yt.share.backend.device;

import iliev.yt.share.backend.device.dto.DeviceInputDto;
import iliev.yt.share.backend.device.dto.DeviceOutputDto;
import iliev.yt.share.backend.device.exception.DeviceNotFoundException;
import iliev.yt.share.backend.user.preferences.UserPreferences;
import iliev.yt.share.backend.user.preferences.UserPreferencesRepository;
import iliev.yt.share.backend.user.preferences.exception.UserPreferencesNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;
    private final UserPreferencesRepository userPreferencesRepository;

    public List<DeviceOutputDto> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(deviceMapper::toOutputDto).toList();
    }

    public Page<DeviceOutputDto> getAllDevices(final Pageable pageable) {
        return deviceRepository.findAll(pageable)
                .map(deviceMapper::toOutputDto);
    }

    public DeviceOutputDto getDeviceById(final UUID id) {
        final Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));
        return deviceMapper.toOutputDto(device);
    }

    public List<DeviceOutputDto> getDevicesByUserPreferencesId(final UUID userPreferencesId) {
        return deviceRepository.findByUserPreferencesId(userPreferencesId).stream()
                .map(deviceMapper::toOutputDto).toList();
    }

    @Transactional
    public DeviceOutputDto createDevice(final DeviceInputDto inputDto) {
        final UserPreferences userPreferences = userPreferencesRepository.findById(inputDto.userPreferencesId())
                .orElseThrow(() -> new UserPreferencesNotFoundException(inputDto.userPreferencesId()));

        final Device device = deviceMapper.toEntity(inputDto);
        device.setUserPreferences(userPreferences);

        final Device savedDevice = deviceRepository.save(device);
        return deviceMapper.toOutputDto(savedDevice);
    }

    @Transactional
    public DeviceOutputDto updateDevice(final UUID id, final DeviceInputDto inputDto) {
        final Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));

        deviceMapper.updateEntity(inputDto, existingDevice);

        final Device updatedDevice = deviceRepository.save(existingDevice);
        return deviceMapper.toOutputDto(updatedDevice);
    }

    @Transactional
    public void deleteDevice(final UUID id) {
        if (!deviceRepository.existsById(id)) {
            throw new DeviceNotFoundException(id);
        }
        deviceRepository.deleteById(id);
    }
}
