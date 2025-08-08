package edu.handong.csee.histudy.controller.form;

import java.util.List;
import lombok.*;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplyFormV2 {

    private List<Long> friendIds;

    private List<Long> courseIds;
}
