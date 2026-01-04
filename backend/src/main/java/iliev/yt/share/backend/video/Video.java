package iliev.yt.share.backend.video;

import iliev.yt.share.backend.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "videos")
public class Video extends BaseEntity {
    private String title;
    private String description;
    private String url;
    private String thumbnailUrl;
}
