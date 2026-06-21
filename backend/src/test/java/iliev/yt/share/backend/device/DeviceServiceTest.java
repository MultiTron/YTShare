package iliev.yt.share.backend.device;

import iliev.yt.share.backend.device.dto.DeviceInputDto;
import iliev.yt.share.backend.device.dto.DeviceOutputDto;
import iliev.yt.share.backend.device.exception.DeviceNotFoundException;
import iliev.yt.share.backend.user.preferences.UserPreferences;
import iliev.yt.share.backend.user.preferences.UserPreferencesRepository;
import iliev.yt.share.backend.user.preferences.exception.UserPreferencesNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @InjectMocks
    private DeviceService deviceService;

    private UUID id;
    private UUID prefsId;
    private Device device;
    private DeviceOutputDto outputDto;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        prefsId = UUID.randomUUID();
        device = Device.builder().hostName("host").ipAddress("127.0.0.1").port("8080").build();
        device.setId(id);
        outputDto = new DeviceOutputDto(id, "host", "127.0.0.1", "8080", null, prefsId);
    }

    private DeviceInputDto inputDto() {
        return new DeviceInputDto("host", "127.0.0.1", "8080", LocalDateTime.now(), prefsId);
    }

    @Test
    void getAllDevices_returnsMappedList() {
        when(deviceRepository.findAll()).thenReturn(List.of(device));
        when(deviceMapper.toOutputDto(device)).thenReturn(outputDto);

        assertThat(deviceService.getAllDevices()).containsExactly(outputDto);
    }

    @Test
    void getAllDevices_paged_returnsMappedPage() {
        final Pageable pageable = PageRequest.of(0, 10);
        when(deviceRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(device)));
        when(deviceMapper.toOutputDto(device)).thenReturn(outputDto);

        final Page<DeviceOutputDto> result = deviceService.getAllDevices(pageable);

        assertThat(result.getContent()).containsExactly(outputDto);
    }

    @Test
    void getDeviceById_found_returnsDto() {
        when(deviceRepository.findById(id)).thenReturn(Optional.of(device));
        when(deviceMapper.toOutputDto(device)).thenReturn(outputDto);

        assertThat(deviceService.getDeviceById(id)).isEqualTo(outputDto);
    }

    @Test
    void getDeviceById_notFound_throws() {
        when(deviceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDeviceById(id))
                .isInstanceOf(DeviceNotFoundException.class);
    }

    @Test
    void getDevicesByUserPreferencesId_returnsMappedList() {
        when(deviceRepository.findByUserPreferencesId(prefsId)).thenReturn(List.of(device));
        when(deviceMapper.toOutputDto(device)).thenReturn(outputDto);

        assertThat(deviceService.getDevicesByUserPreferencesId(prefsId)).containsExactly(outputDto);
    }

    @Test
    void createDevice_prefsFound_setsPrefsSavesAndReturnsDto() {
        final DeviceInputDto inputDto = inputDto();
        final UserPreferences prefs = UserPreferences.builder().build();
        prefs.setId(prefsId);
        when(userPreferencesRepository.findById(prefsId)).thenReturn(Optional.of(prefs));
        when(deviceMapper.toEntity(inputDto)).thenReturn(device);
        when(deviceRepository.save(device)).thenReturn(device);
        when(deviceMapper.toOutputDto(device)).thenReturn(outputDto);

        assertThat(deviceService.createDevice(inputDto)).isEqualTo(outputDto);
        assertThat(device.getUserPreferences()).isEqualTo(prefs);
        verify(deviceRepository).save(device);
    }

    @Test
    void createDevice_prefsNotFound_throwsAndDoesNotSave() {
        final DeviceInputDto inputDto = inputDto();
        when(userPreferencesRepository.findById(prefsId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.createDevice(inputDto))
                .isInstanceOf(UserPreferencesNotFoundException.class);
        verify(deviceRepository, never()).save(device);
    }

    @Test
    void updateDevice_found_updatesSavesAndReturnsDto() {
        final DeviceInputDto inputDto = inputDto();
        when(deviceRepository.findById(id)).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);
        when(deviceMapper.toOutputDto(device)).thenReturn(outputDto);

        assertThat(deviceService.updateDevice(id, inputDto)).isEqualTo(outputDto);
        verify(deviceMapper).updateEntity(inputDto, device);
        verify(deviceRepository).save(device);
    }

    @Test
    void updateDevice_notFound_throws() {
        final DeviceInputDto inputDto = inputDto();
        when(deviceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.updateDevice(id, inputDto))
                .isInstanceOf(DeviceNotFoundException.class);
    }

    @Test
    void deleteDevice_exists_deletes() {
        when(deviceRepository.existsById(id)).thenReturn(true);

        deviceService.deleteDevice(id);

        verify(deviceRepository).deleteById(id);
    }

    @Test
    void deleteDevice_notExists_throwsAndDoesNotDelete() {
        when(deviceRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> deviceService.deleteDevice(id))
                .isInstanceOf(DeviceNotFoundException.class);
        verify(deviceRepository, never()).deleteById(id);
    }
}
