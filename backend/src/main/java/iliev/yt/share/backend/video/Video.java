package iliev.yt.share.backend.video;

import iliev.yt.share.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "videos")
public class Video extends BaseEntity {
    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 1024)
    private String description;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String thumbnailUrl;
}
